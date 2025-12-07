package com.github.ragxar.wurm.client.mod;

import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.shared.util.MaterialUtilities;

public class QuestCandidateItem {
    public long id;
    public String name;
    public long weight;
    public boolean isLiquid;
    public int sended;
    public int added;
    public int poured;
    public boolean running;
    public boolean complete;

    public QuestCandidateItem(InventoryMetaItem item) {
        id = item.getId();
        name = item.getDisplayName();
        weight = (long) item.getWeight() * 1000;
        isLiquid = MaterialUtilities.isLiquid(item.getMaterialId());
        sended = 0;
        added = 0;
        poured = 0;
        complete = false;
        running = false;
    }
}
