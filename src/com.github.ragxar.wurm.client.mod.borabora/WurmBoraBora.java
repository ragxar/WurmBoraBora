package com.github.ragxar.wurm.client.mod.borabora;

import com.wurmonline.client.renderer.gui.HeadsUpDisplay;
import com.wurmonline.client.renderer.gui.MainMenu;
import com.wurmonline.client.renderer.gui.WurmComponent;
import com.wurmonline.client.renderer.gui.QuestWizardWindow;
import com.wurmonline.client.settings.SavePosManager;

import org.gotti.wurmunlimited.modloader.ReflectionUtil;
import org.gotti.wurmunlimited.modloader.classhooks.HookManager;
import org.gotti.wurmunlimited.modloader.interfaces.Initable;
import org.gotti.wurmunlimited.modloader.interfaces.PreInitable;
import org.gotti.wurmunlimited.modloader.interfaces.WurmClientMod;

import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;


public class WurmBoraBora implements WurmClientMod, Initable, PreInitable {
	public static Logger logger = Logger.getLogger("WurmBoraBora");

	public static boolean handleInput(final String cmd, final String[] data) {
		return false;
	}

	@Override
	public void init() {
		HookManager.getInstance().registerHook("com.wurmonline.client.renderer.gui.HeadsUpDisplay", "init", "(II)V", () -> (proxy, method, args) -> {
			method.invoke(proxy, args);

			registerQuestWizardWindow((HeadsUpDisplay) proxy);

			return null;
		});
	}

	@Override
	public void preInit() {
	}

	private void registerQuestWizardWindow(HeadsUpDisplay hud) {
		try {
			QuestWizardWindow questWizardWindow = new QuestWizardWindow();

			MainMenu mainMenu = ReflectionUtil.getPrivateField(hud, ReflectionUtil.getField(hud.getClass(), "mainMenu"));
			List<WurmComponent> components = ReflectionUtil.getPrivateField(hud, ReflectionUtil.getField(hud.getClass(), "components"));
			SavePosManager savePosManager = ReflectionUtil.getPrivateField(hud, ReflectionUtil.getField(hud.getClass(), "savePosManager"));

			mainMenu.registerComponent("Bora Quest Wizard", questWizardWindow);
			mainMenu.setEnabled(questWizardWindow, false);
			savePosManager.registerAndRefresh(questWizardWindow, "bora-bora-quest");

			logger.log(Level.INFO, "Registered BoraLiteQuestWizard window");
		} catch (IllegalArgumentException | IllegalAccessException | ClassCastException | NoSuchFieldException e) {
			throw new RuntimeException(e);
		}
	}
}
