package com.hallowatcher.dialogscraper;

import com.google.inject.Provides;
import javax.inject.Inject;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.*;
import net.runelite.api.events.GameTick;
import net.runelite.api.events.InteractingChanged;
import net.runelite.api.events.MenuOptionClicked;
import net.runelite.api.events.WidgetLoaded;
import net.runelite.api.util.Text;
import net.runelite.api.widgets.Widget;
import net.runelite.api.widgets.WidgetID;
import net.runelite.api.widgets.WidgetInfo;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.input.KeyListener;
import net.runelite.client.input.KeyManager;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;
import org.json.JSONArray;
import org.json.JSONObject;
import org.pf4j.Extension;

import java.awt.event.KeyEvent;
import java.io.FileWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.text.SimpleDateFormat;
import java.util.*;

@Extension
@PluginDescriptor(
	name = "Dialog Scraper",
	description = "A plugin to scrape dialog and quest diary information like text, head animations and options."
)
@Slf4j
public class DialogScraperPlugin extends Plugin implements KeyListener
{
	@Inject
	private DialogScraperConfig config;

	@Inject
	private Client client;

	@Inject
	private KeyManager keyManager;

	private static final String USERNAME_TOKEN = "%USERNAME%";
	private static final String CONTINUE_MENU_OPTION = "Continue";

	private boolean queuePlayerDialog;
	private boolean queueNPCDialog;
	private boolean queueOptionsDialog;
	private boolean queueQuestDiary;

	private JSONArray conversation;
	private String previousDialogText;
	private NPC lastNpcInteracted;
	private Path sessionPath;

	private SimpleDateFormat isoDateFormat = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss.SSSXXX");
	private SimpleDateFormat sessionDateFormat = new SimpleDateFormat("yyyyMMddHHmmss");

	// Provides our config
	@Provides
	DialogScraperConfig provideConfig(ConfigManager configManager)
	{
		return configManager.getConfig(DialogScraperConfig.class);
	}

	@Override
	protected void startUp() throws IOException {
		// runs on plugin startup
		log.info("Dialog Scraper started!");
		createSavePath();
		keyManager.registerKeyListener(this);
	}

	@Override
	protected void shutDown()
	{
		// runs on plugin shutdown
		log.info("Dialog Scraper stopped!");
		keyManager.unregisterKeyListener(this);
	}

	@Subscribe
	private void onInteractingChanged(InteractingChanged event) {
		if (event.getSource() != client.getLocalPlayer())
		{
			return;
		}

		final Actor target = event.getTarget();
		if (!(target instanceof NPC))
		{
			return;
		}

		lastNpcInteracted = (NPC) target;
	}

	@Subscribe
	private void onGameTick(GameTick gameTick) throws IOException {
		// Initiate the conversation array
		if (conversation == null && (queuePlayerDialog || queueNPCDialog || queueOptionsDialog)) {
			log.info("Conversation started");
			conversation = new JSONArray();
		}

		if (queuePlayerDialog) {
			queuePlayerDialog = false;
			onPlayerDialog();
		}

		if (queueNPCDialog) {
			queueNPCDialog = false;
			onNpcDialog();
		}

		if (queueOptionsDialog) {
			queueOptionsDialog = false;
			onOptionsDialog();
		}

		if (queueQuestDiary) {
			queueQuestDiary = false;
			onQuestDiary();
		}

		// Check if the chatbox was loaded in again
		boolean sendMessageHidden = Objects.requireNonNull(client.getWidget(WidgetInfo.CHATBOX_INPUT)).isHidden();
		if (!sendMessageHidden && conversation != null) {
			// End the conversation
			log.info("Conversation ended and saved with " + lastNpcInteracted.getName());

			String sanitizedNpcName = Objects.requireNonNull(lastNpcInteracted.getName()).replace(" ", "_").toLowerCase();
			String fileName = lastNpcInteracted.getId() + "_" + sanitizedNpcName + ".json";
			Path fullFilePath = Paths.get(String.valueOf(sessionPath), fileName);

			JSONArray result = new JSONArray();

			if (Files.exists(fullFilePath)) {
				String existingDataString = new String(Files.readAllBytes(fullFilePath));
				JSONArray existingDialog = new JSONArray(existingDataString);

				for (int i = 0; i < existingDialog.length(); i++) {
					result.put(existingDialog.getJSONArray(i));
				}
			}

			result.put(conversation);

			try (FileWriter file = new FileWriter(fullFilePath.toString(), false)) {
				file.write(result.toString(2));
			}

			// Reset
			previousDialogText = null;
			conversation = null;
		}
	}

	@Subscribe
	private void onWidgetLoaded(WidgetLoaded widget)
	{
		final int widgetGroupId = widget.getGroupId();

		switch (widgetGroupId) {
			case WidgetID.DIALOG_PLAYER_GROUP_ID:
				queuePlayerDialog = true;
				break;
			case WidgetID.DIALOG_NPC_GROUP_ID:
				queueNPCDialog = true;
				break;
			case WidgetID.DIALOG_OPTION_GROUP_ID:
				queueOptionsDialog = true;
				break;
			case WidgetID.DIARY_QUEST_GROUP_ID:
				queueQuestDiary = true;
				break;
		}
	}

	@Subscribe
	private void onMenuOptionClicked(MenuOptionClicked event) {
		if (event.getMenuOption().startsWith(CONTINUE_MENU_OPTION)
		  && event.getMenuAction() == MenuAction.WIDGET_TYPE_6) {
			Widget[] options;
			try {
				options = Objects.requireNonNull(client.getWidget(WidgetInfo.DIALOG_OPTION_OPTION1)).getChildren();
			} catch (Exception ex) {
				return;
			}

			int optionSelected = event.getActionParam();
			if (options == null || options[optionSelected] == null || options[optionSelected].getText().isBlank()) {
				return;
			}

			previousDialogText = options[optionSelected].getText();
		}
	}

	@Override
	public void keyPressed(KeyEvent e)
	{
		Widget[] options;
		try {
			options = Objects.requireNonNull(client.getWidget(WidgetInfo.DIALOG_OPTION_OPTION1)).getChildren();
		} catch (Exception ex) {
			return;
		}

		if (options == null || options.length == 0) {
			return;
		}

		int optionSelected;
		switch (e.getKeyCode()) {
			case KeyEvent.VK_1:
				optionSelected = 1;
				break;
			case KeyEvent.VK_2:
				optionSelected = 2;
				break;
			case KeyEvent.VK_3:
				optionSelected = 3;
				break;
			case KeyEvent.VK_4:
				optionSelected = 4;
				break;
			case KeyEvent.VK_5:
				optionSelected = 5;
				break;
			case KeyEvent.VK_6:
				optionSelected = 6;
				break;
			default:
				return;
		}

		if (options[optionSelected] == null || options[optionSelected].getText().isBlank()) {
			return;
		}

		previousDialogText = options[optionSelected].getText();
	}

	@Override
	public void keyTyped(KeyEvent e) { }

	@Override
	public void keyReleased(KeyEvent e) { }

	private String sanitize(String text)
	{
		String username = client.getLocalPlayer().getName();
		return text.replaceAll(username, USERNAME_TOKEN);
	}

	private void createSavePath() throws IOException {
		Date date = Calendar.getInstance().getTime();
		sessionPath = Paths.get(config.savePath(), sessionDateFormat.format(date));
		Files.createDirectories(sessionPath);
	}

	private void onPlayerDialog() {
		if (!config.scrapeDialog()) {
			return;
		}

		// Build JSON for this chat dialog
		JSONObject obj = createDialogJsonObject(DialogType.Player);
		previousDialogText = sanitize(obj.getString("text"));
		conversation.put(obj);
	}

	private void onNpcDialog() {
		if (!config.scrapeDialog()) {
			return;
		}

		// Build JSON for this chat dialog
		JSONObject obj = createDialogJsonObject(DialogType.NPC);
		previousDialogText = sanitize(obj.getString("text"));
		conversation.put(obj);
	}

	private void onOptionsDialog() {
		if (!config.scrapeDialog()) {
			return;
		}

		// Build JSON for this chat dialog
		JSONObject obj = createDialogJsonObject(DialogType.Options);
		previousDialogText = null;
		conversation.put(obj);
	}

	private JSONObject createDialogJsonObject(DialogType dialogType) {
		String name = null;
		String text = null;
		Integer animation = null;
		JSONArray opts = null;

		switch (dialogType) {
			case Player:
				name = USERNAME_TOKEN;
				text = Objects.requireNonNull(client.getWidget(WidgetInfo.DIALOG_PLAYER_TEXT)).getText();
				animation = Objects.requireNonNull(client.getWidget(WidgetInfo.DIALOG_PLAYER_HEAD_MODEL)).getAnimationId();
				break;
			case NPC:
				name = Objects.requireNonNull(client.getWidget(WidgetInfo.DIALOG_NPC_NAME)).getText();
				text = Objects.requireNonNull(client.getWidget(WidgetInfo.DIALOG_NPC_TEXT)).getText();
				animation = Objects.requireNonNull(client.getWidget(WidgetInfo.DIALOG_NPC_HEAD_MODEL)).getAnimationId();
				break;
			case Options:
				opts = new JSONArray();
				Widget[] options = Objects.requireNonNull(client.getWidget(WidgetInfo.DIALOG_OPTION_OPTION1)).getChildren();
				for (int i = 0; i < Objects.requireNonNull(options).length; i++) {
					String optionText = options[i].getText();

					if (optionText.toLowerCase().contains("select an option") || optionText.isBlank()) {
						continue;
					}

					opts.put(sanitize(optionText));
				}
				break;
		}

		JSONObject obj = new JSONObject();
		obj.put("name", name != null ? name : JSONObject.NULL);
		obj.put("type", dialogType.getDialogType());
		obj.put("text", text != null ? sanitize(Text.sanitizeMultilineText(text)) : JSONObject.NULL);
		obj.put("animation", animation != null ? animation : JSONObject.NULL);
		obj.put("previousText", previousDialogText != null ? previousDialogText : JSONObject.NULL);
		obj.put("options", opts != null ? opts : JSONObject.NULL);

		Date date = Calendar.getInstance().getTime();
		obj.put("date", isoDateFormat.format(date));
		return obj;
	}

	private void onQuestDiary() {
		if (!config.scrapeQuestDiary()) {
			return;
		}

		String questTitle = Objects.requireNonNull(client.getWidget(WidgetInfo.DIARY_QUEST_WIDGET_TITLE)).getText();
		Widget[] questDiaryLines = Objects.requireNonNull(client.getWidget(WidgetInfo.DIARY_QUEST_WIDGET_TEXT)).getStaticChildren();

		// Build JSON for this chat dialog
		JSONObject obj = new JSONObject();
		obj.put("name", questTitle);

		JSONArray opts = new JSONArray();

		for (int i = 0; i < Objects.requireNonNull(questDiaryLines).length; i++) {
			String optionText = questDiaryLines[i].getText();

			if (optionText.isBlank()) {
				continue;
			}

			opts.put(optionText);
		}

		obj.put("lines", opts);

		Date date = Calendar.getInstance().getTime();
		obj.put("date", isoDateFormat.format(date));

		log.info("Quest diary collected:");
		log.info(obj.toString(2));
		// TODO save diary to file
	}
}