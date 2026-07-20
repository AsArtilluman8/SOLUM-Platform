#!/usr/bin/env python3
"""Extract source-backed Blueprint writer control flow and caller topology.

This module does not infer a frame frequency. It preserves authored exec edges and call chains so
runtime scheduling cannot be replaced by an undocumented per-frame or startup-only shortcut.
"""

from __future__ import annotations

import hashlib
import json
from collections import deque
from typing import Any


def _sha256_json(value: object) -> str:
    return hashlib.sha256(
        json.dumps(value, ensure_ascii=False, sort_keys=True).encode("utf-8")
    ).hexdigest()


def _nested_field(value: Any, name: str) -> Any:
    if not isinstance(value, dict):
        return None
    for item in value.get("properties", []):
        if item.get("name") == name:
            return item.get("value")
    return None


def _operation(node: dict[str, Any]) -> str | None:
    return _nested_field(
        node.get("properties", {}).get("FunctionReference"), "MemberName"
    )


def _variable(node: dict[str, Any]) -> str | None:
    return _nested_field(
        node.get("properties", {}).get("VariableReference"), "MemberName"
    )


def _delegate_name(node: dict[str, Any]) -> str | None:
    return _nested_field(
        node.get("properties", {}).get("DelegateReference"), "MemberName"
    )


def _pin_default(node: dict[str, Any], name: str) -> str | None:
    pin = next((item for item in node.get("pins", []) if item.get("name") == name), None)
    return pin.get("default_value") if pin else None


def _graph_name(graph: dict[str, Any]) -> str:
    return graph["graph"].rsplit(".", 1)[-1]


def _exec_edges(graph: dict[str, Any]) -> list[dict[str, Any]]:
    result = []
    for node in graph.get("nodes", []):
        for pin in node.get("pins", []):
            if pin.get("direction") != "output":
                continue
            if pin.get("type", {}).get("category") != "exec":
                continue
            for link in pin.get("linked_to", []):
                result.append(
                    {
                        "fromNode": node.get("export_index"),
                        "fromPin": pin.get("name"),
                        "toNode": link.get("owning_node_index"),
                        "toPinId": link.get("pin_id"),
                    }
                )
    return sorted(
        result,
        key=lambda item: (
            item["fromNode"] or -1,
            item["fromPin"] or "",
            item["toNode"] or -1,
        ),
    )


def _shortest_path(
    adjacency: dict[int, list[int]], starts: list[int], target: int
) -> list[int] | None:
    queue = deque((start, [start]) for start in starts)
    seen = set(starts)
    while queue:
        node, path = queue.popleft()
        if node == target:
            return path
        for neighbor in adjacency.get(node, []):
            if neighbor in seen:
                continue
            seen.add(neighbor)
            queue.append((neighbor, [*path, neighbor]))
    return None


def build_writer_control_flow(
    graph: dict[str, Any], writer_contracts: list[dict[str, Any]]
) -> dict[str, Any]:
    nodes = {node["export_index"]: node for node in graph.get("nodes", [])}
    edges = _exec_edges(graph)
    adjacency: dict[int, list[int]] = {}
    for edge in edges:
        adjacency.setdefault(edge["fromNode"], []).append(edge["toNode"])
    entries = sorted(
        node["export_index"]
        for node in graph.get("nodes", [])
        if node.get("class") == "K2Node_FunctionEntry"
    )
    target_nodes = sorted(
        {
            index
            for writer in writer_contracts
            for index in writer.get("setNodes", [])
        }
    )
    paths = [
        {
            "writerNode": target,
            "parameter": _variable(nodes[target]) if target in nodes else None,
            "shortestExecPathFromEntry": _shortest_path(adjacency, entries, target),
        }
        for target in target_nodes
    ]
    relevant_nodes = sorted(
        {
            index
            for path in paths
            for index in (path["shortestExecPathFromEntry"] or [])
        }
        | set(target_nodes)
    )
    node_rows = [
        {
            "sourceNode": index,
            "sourceName": nodes[index].get("name"),
            "sourceClass": nodes[index].get("class"),
            "operation": _operation(nodes[index]),
            "variable": _variable(nodes[index]),
        }
        for index in relevant_nodes
        if index in nodes
    ]
    unresolved_targets = [
        path["writerNode"] for path in paths if path["shortestExecPathFromEntry"] is None
    ]
    payload = {
        "sourceGraph": graph["graph"],
        "sourceName": _graph_name(graph),
        "status": "VERIFIED" if entries and not unresolved_targets else "PARTIAL",
        "entryNodes": entries,
        "writerNodes": target_nodes,
        "writerPaths": paths,
        "relevantNodes": node_rows,
        "executionEdges": edges,
        "unresolvedWriterNodes": unresolved_targets,
        "semanticLevel": "EXACT_K2_EXEC_TOPOLOGY_NOT_RUNTIME_FREQUENCY",
    }
    payload["topologySha256"] = _sha256_json(payload)
    return payload


def _local_call_graph(graphs: dict[str, dict[str, Any]]) -> dict[str, list[str]]:
    result = {}
    local_names = set(graphs)
    for name, graph in graphs.items():
        result[name] = sorted(
            {
                operation
                for node in graph.get("nodes", [])
                if node.get("class") == "K2Node_CallFunction"
                for operation in [_operation(node)]
                if operation in local_names
            }
        )
    return result


def _delegate_bindings(
    graphs: dict[str, dict[str, Any]],
) -> dict[str, list[dict[str, Any]]]:
    """Resolve authored CreateDelegate bindings without pretending they are direct calls."""
    local_names = set(graphs)
    result: dict[str, list[dict[str, Any]]] = {name: [] for name in graphs}
    for graph_name, graph in graphs.items():
        nodes = {node["export_index"]: node for node in graph.get("nodes", [])}
        edges = _exec_edges(graph)
        adjacency: dict[int, list[int]] = {}
        for edge in edges:
            adjacency.setdefault(edge["fromNode"], []).append(edge["toNode"])
        entries = sorted(
            node["export_index"]
            for node in graph.get("nodes", [])
            if node.get("class") == "K2Node_FunctionEntry"
        )
        for node in graph.get("nodes", []):
            if node.get("class") != "K2Node_CreateDelegate":
                continue
            selected = node.get("properties", {}).get("SelectedFunctionName")
            if selected not in local_names:
                continue
            for pin in node.get("pins", []):
                if pin.get("direction") != "output" or pin.get("type", {}).get("category") != "delegate":
                    continue
                for link in pin.get("linked_to", []):
                    consumer_index = link.get("owning_node_index")
                    consumer = nodes.get(consumer_index, {})
                    consumer_pin_id = link.get("pin_id")
                    consumer_pin = next(
                        (
                            item
                            for item in consumer.get("pins", [])
                            if item.get("reference", {}).get("pin_id") == consumer_pin_id
                        ),
                        None,
                    )
                    result[selected].append(
                        {
                            "bindingGraph": graph_name,
                            "createDelegateNode": node.get("export_index"),
                            "selectedFunction": selected,
                            "consumerNode": consumer_index,
                            "consumerOperation": _operation(consumer),
                            "consumerPin": consumer_pin.get("name") if consumer_pin else None,
                            "consumerUpdateGroup": _pin_default(consumer, "Update Group"),
                            "execPathFromBindingGraphEntryToConsumer": (
                                _shortest_path(adjacency, entries, consumer_index)
                                if isinstance(consumer_index, int)
                                else None
                            ),
                        }
                    )
    for bindings in result.values():
        bindings.sort(
            key=lambda item: (
                item["bindingGraph"],
                item["createDelegateNode"],
                item["consumerNode"] or -1,
            )
        )
    return result


def _delegate_group_map(
    graphs: dict[str, dict[str, Any]],
) -> dict[str, dict[str, Any]]:
    graph = graphs.get("Set Apply Property Event Binding")
    if graph is None:
        return {}
    nodes = {node["export_index"]: node for node in graph.get("nodes", [])}
    result: dict[str, dict[str, Any]] = {}
    for switch in graph.get("nodes", []):
        if switch.get("class") != "K2Node_SwitchEnum":
            continue
        for pin in switch.get("pins", []):
            if pin.get("direction") != "output" or not pin.get("name", "").startswith("NewEnumerator"):
                continue
            targets = [
                nodes.get(link.get("owning_node_index"), {})
                for link in pin.get("linked_to", [])
            ]
            add_targets = [
                target for target in targets if target.get("class") == "K2Node_AddDelegate"
            ]
            if len(add_targets) != 1:
                continue
            target = add_targets[0]
            result[pin["name"]] = {
                "delegate": _delegate_name(target),
                "sourceGraph": _graph_name(graph),
                "switchNode": switch.get("export_index"),
                "addDelegateNode": target.get("export_index"),
            }
    return result


def _delegate_broadcast_sites(
    graphs: dict[str, dict[str, Any]], delegate_name: str
) -> list[dict[str, Any]]:
    result = []
    for graph_name, graph in graphs.items():
        edges = _exec_edges(graph)
        adjacency: dict[int, list[int]] = {}
        for edge in edges:
            adjacency.setdefault(edge["fromNode"], []).append(edge["toNode"])
        entries = sorted(
            node["export_index"]
            for node in graph.get("nodes", [])
            if node.get("class") == "K2Node_FunctionEntry"
        )
        for node in graph.get("nodes", []):
            if node.get("class") != "K2Node_CallDelegate" or _delegate_name(node) != delegate_name:
                continue
            result.append(
                {
                    "graph": graph_name,
                    "callDelegateNode": node.get("export_index"),
                    "execPathFromEntry": _shortest_path(
                        adjacency, entries, node.get("export_index")
                    ),
                }
            )
    return sorted(result, key=lambda item: (item["graph"], item["callDelegateNode"]))


def _call_chains_to_roots(
    target: str, reverse_calls: dict[str, list[str]], max_depth: int = 12
) -> list[list[str]]:
    chains = []
    queue = deque([[target]])
    while queue:
        reverse_path = queue.popleft()
        current = reverse_path[-1]
        callers = [
            caller
            for caller in reverse_calls.get(current, [])
            if caller not in reverse_path
        ]
        if not callers or len(reverse_path) >= max_depth:
            chains.append(list(reversed(reverse_path)))
            continue
        for caller in callers:
            queue.append([*reverse_path, caller])
    return sorted(chains)


def build_writer_schedule_contract(
    blueprint: dict[str, Any], writer_contracts: list[dict[str, Any]]
) -> dict[str, Any]:
    graphs = {
        _graph_name(graph): graph
        for graph in blueprint.get("graph", {}).get("graphs", [])
    }
    by_writer_graph: dict[str, list[dict[str, Any]]] = {}
    for writer in writer_contracts:
        name = writer["sourceGraph"].rsplit(".", 1)[-1]
        by_writer_graph.setdefault(name, []).append(writer)
    control_flow = [
        build_writer_control_flow(graphs[name], writers)
        for name, writers in sorted(by_writer_graph.items())
    ]
    calls = _local_call_graph(graphs)
    delegate_bindings = _delegate_bindings(graphs)
    delegate_group_map = _delegate_group_map(graphs)
    reverse_calls: dict[str, list[str]] = {name: [] for name in graphs}
    for caller, callees in calls.items():
        for callee in callees:
            reverse_calls[callee].append(caller)
    for callers in reverse_calls.values():
        callers.sort()
    schedules = []
    for target in sorted(by_writer_graph):
        call_sites = []
        for caller_name, caller_graph in graphs.items():
            caller_edges = _exec_edges(caller_graph)
            caller_adjacency: dict[int, list[int]] = {}
            for edge in caller_edges:
                caller_adjacency.setdefault(edge["fromNode"], []).append(edge["toNode"])
            caller_entries = sorted(
                node["export_index"]
                for node in caller_graph.get("nodes", [])
                if node.get("class") == "K2Node_FunctionEntry"
            )
            for node in caller_graph.get("nodes", []):
                if node.get("class") != "K2Node_CallFunction" or _operation(node) != target:
                    continue
                exec_pins = [
                    {
                        "name": pin.get("name"),
                        "direction": pin.get("direction"),
                        "links": sorted(
                            link.get("owning_node_index")
                            for link in pin.get("linked_to", [])
                            if isinstance(link.get("owning_node_index"), int)
                        ),
                    }
                    for pin in node.get("pins", [])
                    if pin.get("type", {}).get("category") == "exec"
                ]
                call_sites.append(
                    {
                        "callerGraph": caller_name,
                        "callNode": node.get("export_index"),
                        "execPins": exec_pins,
                        "execPathFromCallerEntry": _shortest_path(
                            caller_adjacency,
                            caller_entries,
                            node.get("export_index"),
                        ),
                    }
                )
        target_delegate_bindings = []
        for binding in delegate_bindings.get(target, []):
            enriched = dict(binding)
            update_group = binding.get("consumerUpdateGroup")
            group_binding = delegate_group_map.get(update_group)
            enriched["resolvedUpdateDelegate"] = group_binding
            enriched["delegateBroadcastSites"] = (
                _delegate_broadcast_sites(graphs, group_binding["delegate"])
                if group_binding and group_binding.get("delegate")
                else []
            )
            enriched["bindingGraphCallChainsToRoots"] = _call_chains_to_roots(
                binding["bindingGraph"], reverse_calls
            )
            target_delegate_bindings.append(enriched)
        schedules.append(
            {
                "writerGraph": target,
                "status": "PARTIAL",
                "directCallers": reverse_calls.get(target, []),
                "callSites": sorted(
                    call_sites,
                    key=lambda item: (item["callerGraph"], item["callNode"]),
                ),
                "delegateBindings": target_delegate_bindings,
                "callChainsToRoots": _call_chains_to_roots(target, reverse_calls),
                "knownTopology": (
                    "VERIFIED"
                    if call_sites
                    or (
                        target_delegate_bindings
                        and all(
                            binding["consumerOperation"]
                            and binding["consumerPin"]
                            and binding["execPathFromBindingGraphEntryToConsumer"] is not None
                            and binding["resolvedUpdateDelegate"]
                            and binding["delegateBroadcastSites"]
                            for binding in target_delegate_bindings
                        )
                    )
                    else "PARTIAL"
                ),
                "unknownRuntimeEvidence": [
                    "which conditional exec path fires first for a concrete runtime state",
                    "timer/delegate cadence and dirty-state transitions",
                    "value visible before the first writer execution",
                    "for delegate bindings, whether the consumer invokes the callback for a concrete changed value",
                ],
            }
        )
    payload = {
        "schema": "solum.p63.10.blueprint-writer-schedule/v1",
        "status": "PARTIAL",
        "controlFlow": control_flow,
        "schedules": schedules,
        "prohibitedSimplifications": [
            "assume every writer executes once at startup",
            "assume every writer executes every frame",
            "discard cache/timer/update-group semantics",
        ],
    }
    payload["contractSha256"] = _sha256_json(payload)
    return payload


def build_operation_order_contract(
    blueprint: dict[str, Any],
    graph_name: str,
    operations: list[str],
    *,
    sequence_semantics_source: str | None = None,
) -> dict[str, Any]:
    graphs = {
        _graph_name(graph): graph
        for graph in blueprint.get("graph", {}).get("graphs", [])
    }
    graph = graphs.get(graph_name)
    if graph is None:
        raise KeyError(f"missing Blueprint graph: {graph_name}")
    nodes = {node["export_index"]: node for node in graph.get("nodes", [])}
    operation_nodes = {
        operation: sorted(
            node["export_index"]
            for node in graph.get("nodes", [])
            if node.get("class") == "K2Node_CallFunction" and _operation(node) == operation
        )
        for operation in operations
    }
    edges = _exec_edges(graph)
    adjacency: dict[int, list[int]] = {}
    for edge in edges:
        adjacency.setdefault(edge["fromNode"], []).append(edge["toNode"])
    entries = sorted(
        node["export_index"]
        for node in graph.get("nodes", [])
        if node.get("class") == "K2Node_FunctionEntry"
    )
    unique = all(len(operation_nodes[operation]) == 1 for operation in operations)
    rows = []
    for operation in operations:
        indices = operation_nodes[operation]
        node_index = indices[0] if len(indices) == 1 else None
        rows.append(
            {
                "operation": operation,
                "node": node_index,
                "sourceName": nodes[node_index].get("name") if node_index else None,
                "pathFromEntry": (
                    _shortest_path(adjacency, entries, node_index)
                    if node_index is not None
                    else None
                ),
            }
        )
    order_paths = []

    def sequence_order(left_path: list[int] | None, right_path: list[int] | None) -> dict[str, Any] | None:
        if not left_path or not right_path:
            return None
        common_length = 0
        for left_node, right_node in zip(left_path, right_path):
            if left_node != right_node:
                break
            common_length += 1
        if common_length == 0 or common_length >= len(left_path) or common_length >= len(right_path):
            return None
        sequence_node = left_path[common_length - 1]
        node = nodes.get(sequence_node, {})
        if node.get("class") != "K2Node_ExecutionSequence":
            return None
        left_next = left_path[common_length]
        right_next = right_path[common_length]

        def output_pin_to(target: int) -> str | None:
            for pin in node.get("pins", []):
                if pin.get("direction") != "output" or pin.get("type", {}).get("category") != "exec":
                    continue
                if any(link.get("owning_node_index") == target for link in pin.get("linked_to", [])):
                    return pin.get("name")
            return None

        left_pin = output_pin_to(left_next)
        right_pin = output_pin_to(right_next)
        if not left_pin or not right_pin or not left_pin.startswith("then_") or not right_pin.startswith("then_"):
            return None
        try:
            left_index = int(left_pin.removeprefix("then_"))
            right_index = int(right_pin.removeprefix("then_"))
        except ValueError:
            return None
        return {
            "sequenceNode": sequence_node,
            "fromBranch": left_pin,
            "toBranch": right_pin,
            "fromBranchIndex": left_index,
            "toBranchIndex": right_index,
            "ordered": left_index < right_index,
            "semantics": "K2Node_ExecutionSequence executes then_N outputs in ascending order",
        }

    for left, right in zip(rows, rows[1:]):
        direct_path = (
            _shortest_path(adjacency, [left["node"]], right["node"])
            if left["node"] is not None and right["node"] is not None
            else None
        )
        sequence_evidence = sequence_order(left["pathFromEntry"], right["pathFromEntry"])
        order_paths.append(
            {
                "fromOperation": left["operation"],
                "toOperation": right["operation"],
                "directPath": direct_path,
                "sequenceOrder": sequence_evidence,
                "status": (
                    "VERIFIED"
                    if direct_path is not None
                    or (sequence_evidence is not None and sequence_evidence["ordered"])
                    else "PARTIAL"
                ),
            }
        )
    status = (
        "VERIFIED"
        if unique
        and entries
        and all(row["pathFromEntry"] is not None for row in rows)
        and all(item["status"] == "VERIFIED" for item in order_paths)
        else "PARTIAL"
    )
    payload = {
        "sourceGraph": graph["graph"],
        "sourceName": graph_name,
        "status": status,
        "entryNodes": entries,
        "operations": rows,
        "orderPaths": order_paths,
        "semanticLevel": "EXACT_K2_EXEC_ORDER_ON_DECODED_PATH_NOT_RUNTIME_BRANCH_RESULT",
        "sequenceSemanticsSource": sequence_semantics_source,
    }
    payload["contractSha256"] = _sha256_json(payload)
    return payload
