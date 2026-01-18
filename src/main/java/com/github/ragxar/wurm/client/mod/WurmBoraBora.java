package com.github.ragxar.wurm.client.mod;

import com.github.ragxar.wurm.client.mod.borabora.Strings;
import com.wurmonline.client.renderer.gui.*;
import com.wurmonline.client.settings.SavePosManager;
import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Configurable;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import java.util.ArrayList;
import java.util.List;
import java.util.Properties;
import java.util.logging.Logger;


public class WurmBoraBora extends InjectionTargetsWurmBoraBora implements WurmClientMod, Initable, PreInitable, Configurable {
	private final static Logger logger = Logger.getLogger(WurmBoraBora.class.getSimpleName());
    
    @Override
	public void init() {
        GameHooks.initialize(this);
    }
    
    @GameHooksMethod(targets = { InventoryListComponent.class })
    public static void registerCrashFixHooks() {
        logger.info("register client crash fix hooks");
        HookManager.getInstance().registerHook(InventoryListComponent.getClassName(), "removeFakeInventoryItem", "(J)V", () -> (proxy, method, args) -> {
            try {
                return method.invoke(proxy, args);
            } catch (Exception ignore) {
                return null;
            }
        });
        
        HookManager.getInstance().registerHook(InventoryListComponent.getClassName(), "removeInventoryItem", "(Lcom/wurmonline/client/game/inventory/InventoryMetaItem;)V", () -> (proxy, method, args) -> {
            try {
                return method.invoke(proxy, args);
            } catch (Exception ignore) {
                return null;
            }
        });
    }

	@Override
	public void preInit() {
	}

    @Override
    public void configure(Properties properties) {
        String language = properties.getProperty("language", "en");
        logger.info("Configure language as " + language);
        Strings.init(language, getClass().getName());
    }
    
    @GameHooksFinallyTask
    @SuppressWarnings("unused")
    public Runnable registerQuestWizardWindow(HeadsUpDisplay hud) {
        return new Runnable() {
            @Override
            public void run() {
                try {
                    logger.info("Registering window...");
                    QuestWizardWindow questWizardWindow = new QuestWizardWindow();
                    
                    ReflectionFieldAccessory hudFieldAccessory = new ReflectionFieldAccessory(hud);
                    MainMenu mainMenu = (MainMenu) hudFieldAccessory.get("mainMenu").orElse(null);
                    @SuppressWarnings("unchecked")
                    List<WurmComponent> components = (List<WurmComponent>) hudFieldAccessory.get("components").orElse(new ArrayList<>());
                    SavePosManager savePosManager = (SavePosManager) hudFieldAccessory.get("savePosManager").orElse(null);
                    
                    if (mainMenu == null) return;
                    mainMenu.registerComponent("Bora Quest Wizard", questWizardWindow);
                    mainMenu.setEnabled(questWizardWindow, false);
                    
                    if (savePosManager == null) return;
                    savePosManager.registerAndRefresh(questWizardWindow, "bora-bora-quest");
                } catch (IllegalArgumentException | ClassCastException e) {
                    logger.severe(e.getMessage());
                }
            }
        };
    }
}
