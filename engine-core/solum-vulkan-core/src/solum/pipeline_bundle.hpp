#pragma once
#include "mesh_resource.hpp"
#include "generated/solum_triangle_vert_spv.h"
#include "generated/solum_triangle_frag_spv.h"

namespace solum {

struct PipelineBundle {
    VkDevice device = VK_NULL_HANDLE;
    VkDescriptorSetLayout textureSetLayout = VK_NULL_HANDLE;
    VkPipelineLayout layout = VK_NULL_HANDLE;
    VkPipeline pipeline = VK_NULL_HANDLE;
    VkPipeline transparentPipeline = VK_NULL_HANDLE;

    void destroy() {
        if (device != VK_NULL_HANDLE) {
            if (transparentPipeline != VK_NULL_HANDLE) vkDestroyPipeline(device, transparentPipeline, nullptr);
            if (pipeline != VK_NULL_HANDLE) vkDestroyPipeline(device, pipeline, nullptr);
            if (layout != VK_NULL_HANDLE) vkDestroyPipelineLayout(device, layout, nullptr);
            if (textureSetLayout != VK_NULL_HANDLE) vkDestroyDescriptorSetLayout(device, textureSetLayout, nullptr);
        }
        transparentPipeline = VK_NULL_HANDLE;
        pipeline = VK_NULL_HANDLE;
        layout = VK_NULL_HANDLE;
        textureSetLayout = VK_NULL_HANDLE;
        device = VK_NULL_HANDLE;
    }

    static VkShaderModule createShaderModule(VkDevice device, const uint32_t* words, size_t wordCount, const char* label, std::string& error) {
        VkShaderModuleCreateInfo ci{ VK_STRUCTURE_TYPE_SHADER_MODULE_CREATE_INFO };
        ci.codeSize = wordCount * sizeof(uint32_t);
        ci.pCode = words;
        VkShaderModule module = VK_NULL_HANDLE;
        VkResult r = vkCreateShaderModule(device, &ci, nullptr, &module);
        if (r != VK_SUCCESS) {
            error = std::string("ShaderModule failed: ") + label + " " + vkResultName(r);
            return VK_NULL_HANDLE;
        }
        return module;
    }

    bool createGraphicsPipeline(VkRenderPass renderPass, VkExtent2D extent, bool transparent, VkShaderModule vert, VkShaderModule frag, VkPipeline& outPipeline, std::string& error) {
        VkPipelineShaderStageCreateInfo stages[2]{};
        stages[0].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
        stages[0].stage = VK_SHADER_STAGE_VERTEX_BIT;
        stages[0].module = vert;
        stages[0].pName = "main";
        stages[1].sType = VK_STRUCTURE_TYPE_PIPELINE_SHADER_STAGE_CREATE_INFO;
        stages[1].stage = VK_SHADER_STAGE_FRAGMENT_BIT;
        stages[1].module = frag;
        stages[1].pName = "main";

        VkVertexInputBindingDescription binding{};
        binding.binding = 0;
        binding.stride = sizeof(Vertex3D);
        binding.inputRate = VK_VERTEX_INPUT_RATE_VERTEX;
        VkVertexInputAttributeDescription attrs[5]{};
        attrs[0].location = 0;
        attrs[0].binding = 0;
        attrs[0].format = VK_FORMAT_R32G32B32_SFLOAT;
        attrs[0].offset = offsetof(Vertex3D, px);
        attrs[1].location = 1;
        attrs[1].binding = 0;
        attrs[1].format = VK_FORMAT_R32G32B32_SFLOAT;
        attrs[1].offset = offsetof(Vertex3D, nx);
        attrs[2].location = 2;
        attrs[2].binding = 0;
        attrs[2].format = VK_FORMAT_R32G32_SFLOAT;
        attrs[2].offset = offsetof(Vertex3D, u);
        attrs[3].location = 3;
        attrs[3].binding = 0;
        attrs[3].format = VK_FORMAT_R32G32B32_SFLOAT;
        attrs[3].offset = offsetof(Vertex3D, r);
        attrs[4].location = 4;
        attrs[4].binding = 0;
        attrs[4].format = VK_FORMAT_R32G32B32A32_SFLOAT;
        attrs[4].offset = offsetof(Vertex3D, tx);
        VkPipelineVertexInputStateCreateInfo vertexInput{ VK_STRUCTURE_TYPE_PIPELINE_VERTEX_INPUT_STATE_CREATE_INFO };
        vertexInput.vertexBindingDescriptionCount = 1;
        vertexInput.pVertexBindingDescriptions = &binding;
        vertexInput.vertexAttributeDescriptionCount = 5;
        vertexInput.pVertexAttributeDescriptions = attrs;

        VkPipelineInputAssemblyStateCreateInfo assembly{ VK_STRUCTURE_TYPE_PIPELINE_INPUT_ASSEMBLY_STATE_CREATE_INFO };
        assembly.topology = VK_PRIMITIVE_TOPOLOGY_TRIANGLE_LIST;
        VkViewport viewport{};
        viewport.width = (float)extent.width;
        viewport.height = (float)extent.height;
        viewport.minDepth = 0.0f;
        viewport.maxDepth = 1.0f;
        VkRect2D scissor{};
        scissor.extent = extent;
        VkPipelineViewportStateCreateInfo viewportState{ VK_STRUCTURE_TYPE_PIPELINE_VIEWPORT_STATE_CREATE_INFO };
        viewportState.viewportCount = 1;
        viewportState.pViewports = &viewport;
        viewportState.scissorCount = 1;
        viewportState.pScissors = &scissor;
        VkPipelineRasterizationStateCreateInfo raster{ VK_STRUCTURE_TYPE_PIPELINE_RASTERIZATION_STATE_CREATE_INFO };
        raster.polygonMode = VK_POLYGON_MODE_FILL;
        raster.cullMode = VK_CULL_MODE_NONE;
        raster.frontFace = VK_FRONT_FACE_COUNTER_CLOCKWISE;
        raster.lineWidth = 1.0f;
        VkPipelineMultisampleStateCreateInfo msaa{ VK_STRUCTURE_TYPE_PIPELINE_MULTISAMPLE_STATE_CREATE_INFO };
        msaa.rasterizationSamples = VK_SAMPLE_COUNT_1_BIT;
        VkPipelineDepthStencilStateCreateInfo depth{ VK_STRUCTURE_TYPE_PIPELINE_DEPTH_STENCIL_STATE_CREATE_INFO };
        depth.depthTestEnable = VK_TRUE;
        depth.depthWriteEnable = transparent ? VK_FALSE : VK_TRUE;
        depth.depthCompareOp = VK_COMPARE_OP_LESS;
        depth.depthBoundsTestEnable = VK_FALSE;
        depth.stencilTestEnable = VK_FALSE;
        VkPipelineColorBlendAttachmentState blendAttachment{};
        blendAttachment.colorWriteMask = VK_COLOR_COMPONENT_R_BIT | VK_COLOR_COMPONENT_G_BIT | VK_COLOR_COMPONENT_B_BIT | VK_COLOR_COMPONENT_A_BIT;
        if (transparent) {
            blendAttachment.blendEnable = VK_TRUE;
            blendAttachment.srcColorBlendFactor = VK_BLEND_FACTOR_SRC_ALPHA;
            blendAttachment.dstColorBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            blendAttachment.colorBlendOp = VK_BLEND_OP_ADD;
            blendAttachment.srcAlphaBlendFactor = VK_BLEND_FACTOR_ONE;
            blendAttachment.dstAlphaBlendFactor = VK_BLEND_FACTOR_ONE_MINUS_SRC_ALPHA;
            blendAttachment.alphaBlendOp = VK_BLEND_OP_ADD;
        }
        VkPipelineColorBlendStateCreateInfo blend{ VK_STRUCTURE_TYPE_PIPELINE_COLOR_BLEND_STATE_CREATE_INFO };
        blend.attachmentCount = 1;
        blend.pAttachments = &blendAttachment;

        VkGraphicsPipelineCreateInfo pipe{ VK_STRUCTURE_TYPE_GRAPHICS_PIPELINE_CREATE_INFO };
        pipe.stageCount = 2;
        pipe.pStages = stages;
        pipe.pVertexInputState = &vertexInput;
        pipe.pInputAssemblyState = &assembly;
        pipe.pViewportState = &viewportState;
        pipe.pRasterizationState = &raster;
        pipe.pMultisampleState = &msaa;
        pipe.pDepthStencilState = &depth;
        pipe.pColorBlendState = &blend;
        pipe.layout = layout;
        pipe.renderPass = renderPass;
        pipe.subpass = 0;
        VkResult r = vkCreateGraphicsPipelines(device, VK_NULL_HANDLE, 1, &pipe, nullptr, &outPipeline);
        if (r != VK_SUCCESS) { error = std::string(transparent ? "Transparent" : "Opaque") + " GraphicsPipeline failed: " + vkResultName(r); return false; }
        return true;
    }

    bool createTrianglePipeline(VkDevice inDevice, VkRenderPass renderPass, VkExtent2D extent, std::string& error) {
        destroy();
        device = inDevice;
        VkShaderModule vert = createShaderModule(device, SOL_TRIANGLE_VERT_SPV, SOL_TRIANGLE_VERT_SPV_WORD_COUNT, "triangle.vert", error);
        if (vert == VK_NULL_HANDLE) return false;
        VkShaderModule frag = createShaderModule(device, SOL_TRIANGLE_FRAG_SPV, SOL_TRIANGLE_FRAG_SPV_WORD_COUNT, "triangle.frag", error);
        if (frag == VK_NULL_HANDLE) { vkDestroyShaderModule(device, vert, nullptr); return false; }

        VkPushConstantRange push{};
        push.stageFlags = VK_SHADER_STAGE_VERTEX_BIT | VK_SHADER_STAGE_FRAGMENT_BIT;
        push.offset = 0;
        push.size = sizeof(PushConstants);
        VkDescriptorSetLayoutBinding samplerBindings[4]{};
        for (uint32_t i = 0; i < 4; ++i) {
            samplerBindings[i].binding = i;
            samplerBindings[i].descriptorCount = 1;
            samplerBindings[i].descriptorType = VK_DESCRIPTOR_TYPE_COMBINED_IMAGE_SAMPLER;
            samplerBindings[i].stageFlags = VK_SHADER_STAGE_FRAGMENT_BIT;
        }
        VkDescriptorSetLayoutCreateInfo setLayoutInfo{ VK_STRUCTURE_TYPE_DESCRIPTOR_SET_LAYOUT_CREATE_INFO };
        setLayoutInfo.bindingCount = 4;
        setLayoutInfo.pBindings = samplerBindings;
        VkResult r = vkCreateDescriptorSetLayout(device, &setLayoutInfo, nullptr, &textureSetLayout);
        if (r != VK_SUCCESS) { error = "Texture DescriptorSetLayout failed: " + vkResultName(r); vkDestroyShaderModule(device, frag, nullptr); vkDestroyShaderModule(device, vert, nullptr); return false; }
        VkPipelineLayoutCreateInfo layoutInfo{ VK_STRUCTURE_TYPE_PIPELINE_LAYOUT_CREATE_INFO };
        layoutInfo.setLayoutCount = 1;
        layoutInfo.pSetLayouts = &textureSetLayout;
        layoutInfo.pushConstantRangeCount = 1;
        layoutInfo.pPushConstantRanges = &push;
        r = vkCreatePipelineLayout(device, &layoutInfo, nullptr, &layout);
        if (r != VK_SUCCESS) { error = "PipelineLayout failed: " + vkResultName(r); vkDestroyShaderModule(device, frag, nullptr); vkDestroyShaderModule(device, vert, nullptr); return false; }

        bool ok = createGraphicsPipeline(renderPass, extent, false, vert, frag, pipeline, error)
            && createGraphicsPipeline(renderPass, extent, true, vert, frag, transparentPipeline, error);
        vkDestroyShaderModule(device, frag, nullptr);
        vkDestroyShaderModule(device, vert, nullptr);
        return ok;
    }
};

} // namespace solum
