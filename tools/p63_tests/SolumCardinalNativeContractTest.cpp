#include "details/CardinalManager.h"

#include <cassert>
#include <cstdint>

using filament::CardinalManager;
using filament::FCardinalManager;

static CardinalManager::Decision decision(uint32_t chunk, uint32_t selected,
        CardinalManager::Representation tier, CardinalManager::Visibility visibility,
        uint64_t revision) {
    CardinalManager::Decision out{};
    out.stableChunkId = chunk;
    out.sourceEntity = chunk * 10u + 1u;
    out.midHqEntity = chunk * 10u + 2u;
    out.midEntity = chunk * 10u + 3u;
    out.farEntity = chunk * 10u + 4u;
    out.selectedEntity = selected;
    out.selectedRepresentation = uint32_t(tier);
    out.selectedMaterialVariant = tier == CardinalManager::Representation::FAR
            ? uint32_t(CardinalManager::MaterialVariant::FAR)
            : tier == CardinalManager::Representation::MID
                ? uint32_t(CardinalManager::MaterialVariant::MID)
                : uint32_t(CardinalManager::MaterialVariant::SOURCE);
    out.visibility = uint32_t(visibility);
    out.flags = CardinalManager::STRUCTURE_STATIC_SAFE;
    out.validityRevision = revision;
    out.decisionRevision = revision;
    out.importance = 1.0f;
    out.sourcePrimitiveCount = 100;
    out.selectedPrimitiveCount = tier == CardinalManager::Representation::SKIP ? 0 : 40;
    return out;
}

int main() {
    FCardinalManager manager;
    CardinalManager::Resident residents[] = {
        { 1, 11, uint32_t(CardinalManager::Representation::SOURCE), 0 },
        { 1, 12, uint32_t(CardinalManager::Representation::MID_HQ), 0 },
        { 1, 13, uint32_t(CardinalManager::Representation::MID), 0 },
        { 1, 14, uint32_t(CardinalManager::Representation::FAR), 0 },
        { 2, 21, uint32_t(CardinalManager::Representation::SOURCE), 0 },
        { 2, 23, uint32_t(CardinalManager::Representation::MID), 0 },
    };
    assert(manager.publishResidents(residents, 6, true));

    CardinalManager::Decision records[] = {
        decision(1, 13, CardinalManager::Representation::MID,
            CardinalManager::Visibility::RENDER, 1),
        decision(2, 0, CardinalManager::Representation::SKIP,
            CardinalManager::Visibility::PROVEN_OUTSIDE_FRUSTUM, 1),
    };
    records[1].midHqEntity = 0;
    records[1].farEntity = 0;
    assert(manager.publishDecisions(records, 2, 1, true));

    manager.requestMode(CardinalManager::Mode::BASELINE_SOURCE, 1);
    manager.beginFrame(1);
    assert(manager.resolveEntity(11).emit);
    assert(!manager.resolveEntity(13).emit);
    assert(manager.resolveEntity(21).emit);
    manager.markPresented();
    auto baseline = manager.getReadback();
    assert(baseline.actualMode == CardinalManager::Mode::BASELINE_SOURCE);
    assert(baseline.presentedModeRevision == baseline.modeRevision);

    manager.requestMode(CardinalManager::Mode::CARDINAL_NATIVE, 2);
    manager.beginFrame(2);
    assert(!manager.resolveEntity(11).emit);
    assert(manager.resolveEntity(13).emit);
    assert(!manager.resolveEntity(21).emit);
    manager.observeResident(11, 2, 100);
    manager.observeResident(21, 1, 50);
    manager.recordPassCommands(FCardinalManager::PassKind::MAIN, 1, 40);
    manager.markPresented();
    auto cardinal = manager.getReadback();
    assert(cardinal.actualMode == CardinalManager::Mode::CARDINAL_NATIVE);
    assert(cardinal.sourceClusters == 0 && cardinal.midClusters == 1);
    assert(cardinal.skippedClusters == 1);
    assert(cardinal.candidateCommands == 3 && cardinal.emittedCommands == 1);
    assert(cardinal.commandsRemoved == 2);
    assert(cardinal.candidatePrimitives == 150 && cardinal.emittedPrimitives == 40);

    // Invalid generation must be SOURCE + RENDER, never missing content.
    records[0].selectedEntity = 14;
    assert(!manager.publishDecisions(records, 2, 2, true));
    manager.requestMode(CardinalManager::Mode::CARDINAL_NATIVE, 3);
    manager.beginFrame(3);
    assert(manager.getReadback().actualMode == CardinalManager::Mode::BASELINE_SOURCE);
    assert(manager.resolveEntity(11).emit && !manager.resolveEntity(13).emit);

    // Restore a valid immutable generation and prove Structure mode/frozen generation identity.
    records[0] = decision(1, 13, CardinalManager::Representation::MID,
        CardinalManager::Visibility::RENDER, 3);
    assert(manager.publishDecisions(records, 2, 3, true));
    manager.requestMode(CardinalManager::Mode::CARDINAL_NATIVE_STRUCTURE_REUSE, 4);
    manager.beginFrame(4);
    assert(manager.structureReuseRequested());
    assert(manager.structureDecisionSafe());
    uint64_t const frozenRevision = manager.getReadback().decisionBufferRevision;
    manager.beginFrame(5);
    assert(manager.getReadback().decisionBufferRevision == frozenRevision);

    // Repeated atomic toggles do not allocate decision generations.
    uint64_t const generations = manager.getReadback().generationCount;
    for (uint64_t i = 0; i < 32; ++i) {
        manager.requestMode(i & 1u ? CardinalManager::Mode::CARDINAL_NATIVE
                                  : CardinalManager::Mode::BASELINE_SOURCE, 10 + i);
        manager.beginFrame(10 + i);
    }
    assert(manager.getReadback().generationCount == generations);
    return 0;
}
