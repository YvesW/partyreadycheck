package com.scainburger.partyreadycheck;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

@ConfigGroup("partyreadycheck")
public interface PartyReadyCheckConfig extends Config
{
	@ConfigItem(
			position = 1,
			keyName = "useAlternateSounds",
			name = "Use alternate sounds",
			description = "Place custom sounds in your \".runelite/partyreadycheck\" folder to use them.<br><br>" +
					"Valid sound files:<br>" +
					"\"start.wav\" - Played when ready check begins<br>" +
					"\"success.wav\" - Played when all players are ready<br>" +
					"\"fail.wav\" - Played when ready check times ot <b> "
	)
	default boolean useAlternateSounds() { return false; }


	@ConfigItem(
			keyName = "readyMessages",
			name = "Ready messages",
			description = "Ready messages to match. Comma,separated,input. Wildcards (*) are accepted.",
			position = 2
	)
	default String readyMessages()
	{
		return "r,rr,rrr*,ready,I'm ready,I'm r,Im ready,Im r,pot,send it,readyy,readyyy*,I'm readyy,I'm readyyy*,Im readyy,Im readyyy*,r.*,rr.*,ready.*,I'm ready.*,I'm r.*,Im ready.*,Im r.*,pot.*,send it.*,readyy.*,I'm readyy.*,Im readyy.*,r!*,rr!*,ready!*,I'm ready!*,I'm r!*,Im ready!*,Im r!*,pot!*,send it!*,readyy!*,I'm readyy!*,Im readyy!*";
	}

	@ConfigItem(
			keyName = "unreadyMessages",
			name = "Unready messages",
			description = "Unready messages to match. Comma,separated,input. Wildcards (*) are accepted.",
			position = 2
	)
	default String unreadyMessages()
	{
		return "unr,unrr,unrrr*,unready,un r,un rr,un rrr*,un ready,not ready,I'm not ready,Im not ready,unr.*,unrr.*,unready.*,un r.*,un rr.*,un ready.*,not ready.*,I'm not ready.*,Im not ready.*,unr!*,unrr!*,unready!*,un r!*,un rr!*,un ready!*,not ready!*,I'm not ready!*,Im not ready!*";
	}
}