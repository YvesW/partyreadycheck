package com.scainburger.partyreadycheck;

import com.google.inject.Provides;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import net.runelite.api.ChatMessageType;
import net.runelite.api.Client;
import net.runelite.api.SoundEffectID;
import net.runelite.api.events.ChatMessage;
import net.runelite.api.events.GameTick;
import net.runelite.api.widgets.Widget;
import net.runelite.client.RuneLite;
import net.runelite.client.chat.ChatColorType;
import net.runelite.client.chat.ChatMessageBuilder;
import net.runelite.client.chat.ChatMessageManager;
import net.runelite.client.chat.QueuedMessage;
import net.runelite.client.config.ConfigManager;
import net.runelite.client.eventbus.Subscribe;
import net.runelite.client.events.ConfigChanged;
import net.runelite.client.plugins.Plugin;
import net.runelite.client.plugins.PluginDescriptor;

import javax.inject.Inject;
import javax.sound.sampled.*;
import java.io.*;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Locale;

@PluginDescriptor(
		name = "Party Ready Check",
		description = "Display an alert to check ready status of your party in ToB or ToA",
		tags = {"tob","theatre","theater","blood","toa","tombs","amascut","party","ready","check"}
)

@Slf4j
public class PartyReadyCheckPlugin extends Plugin {

	final int TOB_HEADER_WIDGET_ID = 1835019;
	final int TOA_HEADER_WIDGET_ID = 50659332;
	final int TOB_PARTY_WIDGET_ID = 1835020;
	final int TOA_PARTY_WIDGET_ID = 50659333;

	Widget tobRaidingPartyHeader;
	Widget toaRaidingPartyHeader;
	Widget raidingPartyWidget = null;

	@Getter
	@Inject
	private Client client;

	@Inject
	private PartyReadyCheckConfig config;

	private int rcTicksRemaining = -1;

	@Subscribe
	public void onGameTick(GameTick tick) {
		if (rcTicksRemaining == 0) {
			rcTicksRemaining = -1;
			playSound("fail.wav", SoundEffectID.PRAYER_DEACTIVE_VWOOP);
			sendChatMessage("The ready check timed out.");
			resetFrame();
		}
		if (rcTicksRemaining > 0 ) {
			rcTicksRemaining--;
		}
	}

	//
	// Below taken from DailyTasksPlugin
	@Inject
	private ChatMessageManager chatMessageManager;

	private void sendChatMessage(String chatMessage)
	{
		final String message = new ChatMessageBuilder()
				.append(ChatColorType.HIGHLIGHT)
				.append(chatMessage)
				.build();

		chatMessageManager.queue(
				QueuedMessage.builder()
						.type(ChatMessageType.CONSOLE)
						.runeLiteFormattedMessage(message)
						.build());
	}
	// End stolen code :)
	//

	@Subscribe
	public void onConfigChanged(ConfigChanged event) throws IOException {
		if (
			config.useAlternateSounds()
			&& !Files.isDirectory(Paths.get(RuneLite.RUNELITE_DIR + "/partyreadycheck"))
		) {
			Files.createDirectory(Paths.get(RuneLite.RUNELITE_DIR + "/partyreadycheck"));
		}
	}

	public void playSound(String customSound, int fallbackSound)
	{
		File soundDir = new File(RuneLite.RUNELITE_DIR,"partyreadycheck/" + customSound);

		if (!config.useAlternateSounds() || !Files.exists(soundDir.toPath())) {
			if (config.useAlternateSounds())
				log.info("\"Use alternate sounds\" is on, but a custom sound file is missing: " + soundDir);
			client.playSoundEffect(fallbackSound);
			return;
		}

		Clip clip = null;
		try {
			clip = AudioSystem.getClip();
			InputStream fileStream = new BufferedInputStream(
					new FileInputStream(soundDir)
			);
			AudioInputStream inputStream = AudioSystem.getAudioInputStream(fileStream);
			clip.open(inputStream);
			if (clip.isControlSupported(FloatControl.Type.MASTER_GAIN)) {
				BooleanControl muteControl = (BooleanControl) clip.getControl(BooleanControl.Type.MUTE);
				muteControl.setValue(false);
				FloatControl gainControl = (FloatControl) clip.getControl(FloatControl.Type.MASTER_GAIN);
				int soundVol = (int) Math.round( client.getPreferences().getSoundEffectVolume() / 1.27);
				float newVol = (float) (Math.log((double) soundVol/100) / Math.log(10.0) * 20.0);
				gainControl.setValue(newVol);
			}
			clip.start();
			Clip finalClip = clip;
			clip.addLineListener(new LineListener() {
				public void update(LineEvent myLineEvent) {
					if (myLineEvent.getType() == LineEvent.Type.STOP)
						finalClip.close();
				}
			});
			return;
		} catch (Exception e) {
			log.warn("Could not play custom sound file: " + e.getMessage() + " (" + soundDir + ")");
			clip.close();
			client.playSoundEffect(fallbackSound);
		}
	}

	@Subscribe
	public void onChatMessage(ChatMessage chatMessage)
	{
		// Don't process messages with no sender i.e. game/system messages
		if (chatMessage.getName().equals(""))
			return;

		tobRaidingPartyHeader = client.getWidget(TOB_HEADER_WIDGET_ID);
		toaRaidingPartyHeader = client.getWidget(TOA_HEADER_WIDGET_ID);
		raidingPartyWidget = null;

		if (
			tobRaidingPartyHeader != null &&
			!(tobRaidingPartyHeader.getText().toUpperCase(Locale.ROOT).equals("NO PARTY")) &&
			!tobRaidingPartyHeader.isHidden()
		) {
			raidingPartyWidget = client.getWidget(TOB_PARTY_WIDGET_ID);
		}
		if (
			toaRaidingPartyHeader != null &&
			!(toaRaidingPartyHeader.getText().toUpperCase(Locale.ROOT).equals("NO PARTY")) &&
			!toaRaidingPartyHeader.isHidden()
		) {
			raidingPartyWidget = client.getWidget(TOA_PARTY_WIDGET_ID);
		}

		if (raidingPartyWidget == null)
			return; // no party

		String msg = chatMessage.getMessage().toUpperCase(Locale.ROOT).trim();
		if (msg.equals("R") || msg.equals("UN R"))
		{
			if (raidingPartyWidget == null || raidingPartyWidget.isHidden())
				return;

			String[] playerNames = raidingPartyWidget.getText().split("<br>");
			StringBuilder outputText = new StringBuilder();

			// Replace nbsp
			String senderName = chatMessage.getName().replace("\u00A0", " ");
			// Remove icons (ironman etc)
			senderName = senderName.replaceAll("\\<img=[0-9]+\\>", "");

			for (int i = 0; i < playerNames.length; i++)
			{
				String name = playerNames[i];

				if (name.equals(senderName) && msg.equals("R"))
				{ // Player is now ready
					outputText.append(name).append(" (R)");

					if (rcTicksRemaining == -1) {
						rcTicksRemaining = 100;
						sendChatMessage(name + " has started a ready check.");
						playSound("start.wav", SoundEffectID.GE_ADD_OFFER_DINGALING);
					}

				}
				else if (name.equals(senderName + " (R)") && msg.equals("UN R"))
				{ // Ready player is no longer ready
					outputText.append(senderName); // restore to original
				}
				else
				{ // No state change
					outputText.append(name);
				}
				if (i < playerNames.length - 1) outputText.append("<br>");
			}

			raidingPartyWidget.setText(outputText.toString());

			playerNames = raidingPartyWidget.getText().split("<br>");
			for (int i = 0; i < playerNames.length; i++)
			{
				String name = playerNames[i];
				if (!name.equals("-") && !name.endsWith(" (R)")) {
					// If anyone not ready, stop
					return;
				}
			}

			// If we get here all players are ready
			sendChatMessage("All party members are ready!");
			playSound("success.wav", SoundEffectID.GE_COIN_TINKLE);
			resetFrame();
			rcTicksRemaining = -1;
		}
	}

	private void resetFrame() {
		String[] playerNames = raidingPartyWidget.getText().split("<br>");
		String outputText = "";

		for (int i = 0; i < playerNames.length; i++) {
			String name = playerNames[i];
			if (name.endsWith(" (R)")) {
				name = name.substring(0, name.length() - 4);
			}
			outputText = outputText + name;
			if (i < playerNames.length - 1) outputText += "<br>";
		}
		raidingPartyWidget.setText(outputText);
	}

	@Provides
	PartyReadyCheckConfig getConfig(ConfigManager configManager)
	{
		return configManager.getConfig(PartyReadyCheckConfig.class);
	}
}