<?php

namespace Jurigag;

class CampaignGroupId
{
    public function getId()
    {
        return 345;
    }
}

class CampaignGroup
{
    public function getCampaignGroupId()
    {
        return new CampaignGroupId();
    }
}

class CampaignGroupQueryService
{
    public function getCampaignGroupsQueryByBrandMasterId($masterBrandId)
    {
        return [new CampaignGroup];
    }
}

class SomeCls
{
    /** @var CampaignGroupQueryService */
    private $campaignGroupQueryService;

    public function main()
    {
        $masterBrandId = 123;
        $campaignGroups = $this->campaignGroupQueryService->getCampaignGroupsQueryByBrandMasterId($masterBrandId);
        $data = [];

        foreach ($campaignGroups as $campaignGroup) {
            // when you complete the 'getCampaignGroupId', caret should end up _after_
            // the parentheses since this function does not take any arguments
            $data[] = $campaignGroup->g;
        }
    }
}
