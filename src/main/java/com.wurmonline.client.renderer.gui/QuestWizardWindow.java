package com.wurmonline.client.renderer.gui;

import com.github.ragxar.wurm.client.mod.ItemSelectionFrame;
import com.github.ragxar.wurm.client.mod.QuestCandidateItem;

import com.github.ragxar.wurm.client.mod.borabora.Strings;
import com.wurmonline.client.game.inventory.InventoryMetaListener;
import com.wurmonline.client.options.Options;
import com.wurmonline.client.game.inventory.InventoryMetaItem;
import com.wurmonline.client.game.inventory.InventoryMetaWindowManager;
import com.wurmonline.client.game.inventory.InventoryMetaWindowView;
import com.wurmonline.client.renderer.PickData;
import com.wurmonline.client.renderer.backend.Queue;
import com.wurmonline.client.renderer.gui.text.TextFont;
import com.wurmonline.client.resources.textures.ResourceTexture;
import com.wurmonline.client.resources.textures.ResourceTextureLoader;
import com.wurmonline.shared.constants.PlayerAction;
import com.wurmonline.shared.util.ItemTypeUtilites;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;

import java.util.*;
import java.util.stream.Collectors;

public class QuestWizardWindow extends WWindow implements InventoryMetaListener {
	private static final String TITLE = Strings.window.title.localized();
	private final String questNamePrefix = "Quest: ";
	private final String selectCountItemsHint = Strings.items.selectedFormat.localized();
	private final String itemStatusFormat = Strings.items.statusFormat.localized();
	private final String emptyCompatibleItems = Strings.items.emptyCompatible.localized();
	private final String questHint = Strings.items.startedHint.localized();
	private final PlayerAction addItemAction = new PlayerAction("questadditem", (short) 951, PlayerAction.ANYTHING);

	private final WurmLabel hintLabel = new WurmLabel(questHint);
	private final WurmLabel statusLabel = new WurmLabel("");
	private final ItemSelectionFrame quest = new ItemSelectionFrame(14, 20, Strings.tooltip.quest.localized());
	private final ItemSelectionFrame items = new ItemSelectionFrame(104, 20, Strings.tooltip.items.localized());

	private final InventoryMetaWindowView playerInventory;
	private WurmArrayPanel<FlexComponent> mainLayout;
	private ResourceTexture playerBackground;

	private int startAddItemsIndex;
	private int maxActionNumber;
	private long lastActionSendTime;
	private final long sendActionTimeout = 250;

	private long lastSearchQuestItemsTime;
	private final long searchNewQuestItemsTimeout = 10000;
	private final Collection<QuestCandidateItem> questCandidates = new ArrayList<>();
	private final Collection<InventoryMetaItem> selectedItems = new ArrayList<>();
	private final Collection<InventoryMetaItem> itemCandidates = new ArrayList<>();
	private InventoryMetaItem selectedQuest;
	
	public QuestWizardWindow() {
		super(TITLE, true);
		setTitle(TITLE);
		resizable = false;

		InventoryMetaWindowManager inventoryManager = hud.getWorld().getInventoryManager();
		playerInventory = inventoryManager.getPlayerInventory();
		playerInventory.addItemListener(this);

	    setupUI();
	}

	private void setupUI() {
		resizable = true;
		closeable = true;
		setInitialSize(380, 124, false);

		text = TextFont.getFixedSizeText();

		mainLayout = new WurmArrayPanel(0);
		mainLayout.addComponent(hintLabel);
		mainLayout.addComponent(new WurmPanel(0, 48, false));
		mainLayout.addComponent(statusLabel);
		setComponent(mainLayout);

		String themeName = Options.guiSkins.options[Options.guiSkins.value()].toLowerCase(Locale.ENGLISH).replace(" ", "");
		playerBackground = ResourceTextureLoader.getNearestTexture("img.gui.background.".concat(themeName));
	}

	public void gameTick() {
		if (selectedQuest == null) {
			if (quest.getTexture() != null) {
				quest.clearTexture();
				hintLabel.setLabel(questHint);
				statusLabel.setLabel("");
			}
			quest.itemsCount = 0;
			return;
		}

		if (selectedItems.isEmpty()) {
			if (items.getTexture() != null) {
				items.clearTexture();
				statusLabel.setLabel("");
			}
			items.itemsCount = 0;
		}

		if (hud.fightWindow.getFighting() || !hud.progressDone()) return;
		if (questCandidates.isEmpty()) return;

		long currentTime = System.currentTimeMillis();

		if ((currentTime - lastSearchQuestItemsTime) > searchNewQuestItemsTimeout) {
			lastSearchQuestItemsTime = currentTime;

			selectedItems.removeIf(Objects::isNull);
			// For supporting not player inventory containers
			updateCandidateItemsHierarchy();
		}

		if ((currentTime - lastActionSendTime) > sendActionTimeout) {
			lastActionSendTime = currentTime;

			updateRunningCandidates();

			if (itemCandidates.isEmpty()) return;

			maxActionNumber = getMaxActionNumber();
			startAddItemsIndex = countOfSendedItems();

			addItemsToQuest(selectedQuest, itemCandidates);
		}
	}

	public void closePressed() {
		toggle();
	}
	
	public void toggle() {
		hud.toggleComponent(this);
	}

	void childResized(FlexComponent child) {
		layout();
	}

	public final void itemDropped(int xMouse, int yMouse, DraggableComponent draggedObject) {
		InventoryMetaItem itemToAdd = null;
		Collection<InventoryMetaItem> droppedItems = new ArrayList<>();
		if (draggedObject instanceof InventoryListComponent.InventoryTreeListItem) {
			InventoryListComponent.InventoryTreeListItem draggedItem = (InventoryListComponent.InventoryTreeListItem)draggedObject;
			itemToAdd = draggedItem.item;
			droppedItems.addAll(
				draggedItem.getSelectedItems().stream()
					.map(treeListItem -> treeListItem.item)
					.collect(Collectors.toList())
			);
		} else if (draggedObject instanceof InventoryContainerWindow.InventoryContainerItem) {
			InventoryContainerWindow.InventoryContainerItem draggedItem = (InventoryContainerWindow.InventoryContainerItem)draggedObject;
			itemToAdd = draggedItem.getItem();
			droppedItems.addAll(
				draggedItem.getSelectedItems().stream()
					.map(containerItem -> containerItem.getItem())
					.collect(Collectors.toList())
			);
		} else if (draggedObject instanceof ToolBeltComponent.ToolBeltItem) {
			ToolBeltComponent.ToolBeltItem draggedItem = (ToolBeltComponent.ToolBeltItem)draggedObject;
			itemToAdd = draggedItem.getItem();
			droppedItems.add(draggedItem.getItem());
		}

		addItemToFrame(itemToAdd, droppedItems, xMouse, yMouse);
	}

	private void updateRunningCandidates() {
		questCandidates.stream()
				.filter(candidateItem -> candidateItem.running)
				.forEach(candidateItem -> {
					InventoryMetaItem selectedItem = itemCandidates.stream()
							.filter(item -> item.getId() == candidateItem.id)
							.findFirst()
							.orElse(null);
					if (selectedItem == null) {
						if (candidateItem.isLiquid) {
							candidateItem.poured++;
						} else {
							candidateItem.added++;
						}
						candidateItem.running = false;
						candidateItem.complete = true;
					} else if (candidateItem.weight > (long) selectedItem.getWeight() * 1000) {
						candidateItem.poured++;
						candidateItem.weight = (long) selectedItem.getWeight() * 1000;
						candidateItem.running = false;
					}
				});

		int countOfAddedItems = countOfAddedItems();
		int countOfPouredItems = countOfPouredItems();
		boolean allSended = questCandidates.stream()
				.allMatch(candidate -> candidate.sended > 0);

		if ((countOfPouredItems + countOfAddedItems) > 0) {
			statusLabel.setLabel(String.format(itemStatusFormat, countOfPouredItems + countOfAddedItems));
		} else if (allSended && !itemCandidates.isEmpty() && (countOfPouredItems + countOfAddedItems) == 0) {
			statusLabel.setLabel(emptyCompatibleItems);
		}
	}

	private int countOfAddedItems() {
		return questCandidates.stream()
				.map(candidate -> candidate.added)
				.reduce(0, Integer::sum);
	}

	private int countOfPouredItems() {
		return questCandidates.stream()
				.map(candidate -> candidate.poured)
				.reduce(0, Integer::sum);
	}

	private int countOfSendedItems() {
		return questCandidates.stream()
				.map(candidate -> candidate.sended)
				.reduce(0, Integer::sum);
	}

	private boolean checkIfItemIsGroup(InventoryMetaItem item) {
		if (item == null) return false;

		return
				ItemTypeUtilites.isContainer(item.getTypeBits()) ||
				ItemTypeUtilites.isInventoryGroup(item.getTypeBits());
	}

	private void addItemToFrame(InventoryMetaItem itemToAdd, Collection<InventoryMetaItem> droppedItems, int xMouse, int yMouse) {
		if (itemToAdd == null) return;

		if (quest.checkIfSlotHovered(xMouse, yMouse)) {
			addToFrameQuest(itemToAdd);
		}
		if (items.checkIfSlotHovered(xMouse, yMouse)) {
			addToFrameItems(itemToAdd, droppedItems);
		}
	}

	private void updateSelection(ItemSelectionFrame itemSelectionFrame, InventoryMetaItem item) {
		itemSelectionFrame.clearTexture();
		itemSelectionFrame.setTexture(item);
	}

	private void addToFrameQuest(InventoryMetaItem itemToAdd) {
		if (!isQuestItem(itemToAdd)) return;

		updateSelection(quest, itemToAdd);
		hintLabel.setLabel(itemToAdd.getBaseName());
		selectedQuest = itemToAdd;
		quest.itemsCount = 1;
	}

	private void addToFrameItems(InventoryMetaItem itemToAdd, Collection<InventoryMetaItem> droppedItems) {
		if (isQuestItem(itemToAdd)) return;

		updateSelection(items, itemToAdd);

		questCandidates.clear();
		selectedItems.clear();
		selectedItems.addAll(droppedItems);
		updateCandidateItemsHierarchy();

		statusLabel.setLabel(String.format(selectCountItemsHint, itemCandidates.size()));
		items.itemsCount = itemCandidates.size();
	}

	protected void renderComponent(Queue queue, float alpha) {
		renderResourceTexture(queue, x + 3, y + 21, width - 6, statusLabel.y, playerBackground);
		super.renderComponent(queue, alpha);
		renderFrames(queue);
		quest.renderText(queue, text);
		items.renderText(queue, text);
	}

	private void renderResourceTexture(Queue queue, int xPosition, int yPosition, int width1, int height1, ResourceTexture texture) {
		Renderer.texturedQuadAlphaBlend(queue, texture, r, g, b, 1.0F, (float)xPosition, (float)yPosition, (float)width1, (float)height1, 0.0F, 0.0F, 1.0F, 1.0F);
	}

	private void renderFrames(Queue queue) {
		quest.render(queue, x, y);
		items.render(queue, x, y);
	}

    public void pick(PickData pickData, int xMouse, int yMouse) {
        quest.pick(pickData, xMouse, yMouse);
        items.pick(pickData, xMouse, yMouse);
    }

	protected void leftPressed(int xMouse, int yMouse, int clickCount) {
		quest.leftPressed(xMouse, yMouse);
		items.leftPressed(xMouse, yMouse);
	}

	protected void leftReleased(int xMouse, int yMouse) {
		boolean hasQuest = quest.getTexture() != null;
		quest.leftReleased(xMouse, yMouse);
		if (hasQuest && quest.getTexture() == null) {
			selectedQuest = null;
			hintLabel.setLabel(questHint);
			if (selectedItems.isEmpty()) {
				statusLabel.setLabel("");
			}
		}

		boolean hasItems = items.getTexture() != null;
		items.leftReleased(xMouse, yMouse);
		if (hasItems && items.getTexture() == null) {
			selectedItems.clear();
			itemCandidates.clear();
			questCandidates.clear();
			statusLabel.setLabel("");
		}
	}

	private boolean isQuestItem(InventoryMetaItem item) {
		return item.getBaseName().startsWith(questNamePrefix);
	}

	private boolean isContainerOrGroup(InventoryMetaItem item) {
		return
			ItemTypeUtilites.isContainer(item.getTypeBits()) ||
			ItemTypeUtilites.isInventoryGroup(item.getTypeBits());
	}

	private void updateCandidateItemsHierarchy() {
		itemCandidates.clear();
		updateCandidateItems(selectedItems);
		items.itemsCount = itemCandidates.size();
	}

	private void updateCandidateItems(Collection<InventoryMetaItem> items) {
		items.forEach(item -> {
			if (isContainerOrGroup(item)) {
				updateCandidateItems(item.getChildren());
				return;
			}

			QuestCandidateItem candidateItem = questCandidates.stream()
					.filter(candidate -> candidate.id == item.getId())
					.findFirst()
					.orElse(null);

			if (candidateItem == null) {
				questCandidates.add(new QuestCandidateItem(item));
			}
			itemCandidates.add(item);
		});
	}

	private void addItemsToQuest(InventoryMetaItem questItemCandidate, Collection<InventoryMetaItem> items) {
		items.forEach(item -> {
			QuestCandidateItem candidateItem = questCandidates.stream()
					.filter(candidate -> candidate.id == item.getId())
					.findFirst()
					.orElse(null);

			if (candidateItem == null) return;
			if (candidateItem.running) return;
			if (!candidateItem.isLiquid && candidateItem.sended > maxActionNumber) return;

			int numberOfActions = countOfSendedItems() - startAddItemsIndex;
			if (numberOfActions > maxActionNumber) return;

			candidateItem.running = true;
			candidateItem.sended++;

			hud.getWorld().getServerConnection().sendAction(
					questItemCandidate.getId(),
					new long[] { item.getId() },
					addItemAction
			);

			lastSearchQuestItemsTime = System.currentTimeMillis() + sendActionTimeout - searchNewQuestItemsTimeout;
		});
	}

	private int getMaxActionNumber() {
		MindLogicCalculator mlc;
		try {
			mlc = ReflectionUtil.getPrivateField(hud,
					ReflectionUtil.getField(hud.getClass(), "mindLogicCalculator"));
		} catch (IllegalAccessException | NoSuchFieldException e) {
			e.printStackTrace();
			return 0;
		}
		return mlc.getMaxNumberOfActions();
	}

	// InventoryMetaListener

	public void addInventoryItem(InventoryMetaItem item) {
		if (selectedItems.isEmpty()) return;

		itemCandidates.clear();
		updateCandidateItems(selectedItems);
		items.itemsCount = itemCandidates.size();
	}

	public void removeInventoryItem(InventoryMetaItem item) {
		if (selectedQuest != null && item.getId() == selectedQuest.getId()) {
			selectedQuest = null;
			hintLabel.setLabel(questHint);

			return;
		}

		if (selectedItems.isEmpty()) return;

		itemCandidates.removeIf(candidate -> candidate.getId() == item.getId());
		items.itemsCount = itemCandidates.size();
		selectedItems.removeIf(selectedItem -> selectedItem.getId() == item.getId());
	}

	public void updateInventoryItem(InventoryMetaItem item) {}

	public void addFakeInventoryItem(long item) {}

	public void removeFakeInventoryItem(long id) {}
}
