/*
 * Copyright (c) 2018, Andrew EP | ElPinche256 <https://github.com/ElPinche256>
 * All rights reserved.
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS" AND
 * ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT OWNER OR CONTRIBUTORS BE LIABLE FOR
 * ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF THIS
 * SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package com.hallowatcher.dialogscraper;

import net.runelite.client.config.Config;
import net.runelite.client.config.ConfigGroup;
import net.runelite.client.config.ConfigItem;

import java.nio.file.Paths;

@ConfigGroup("JavaExampleConfig")

public interface DialogScraperConfig extends Config
{
	@ConfigItem(
		keyName = "scrapeDialog",
		name = "Scrape dialogs",
		description = "Save each detected dialog, along with animation IDs and options",
		position = 0
	)
	default boolean scrapeDialog()
	{
		return true;
	}

	@ConfigItem(
			keyName = "scrapeQuestDiary",
			name = "Scrape quest diaries",
			description = "Scrape the information inside the quest diary",
			position = 1
	)
	default boolean scrapeQuestDiary()
	{
		return true;
	}

	@ConfigItem(
			keyName = "savePath",
			name = "Path to save",
			description = "Path to save your chat dialogs and quest diaries",
			position = 2
	)
	default String savePath()
	{
		return String.valueOf(Paths.get(System.getProperty("user.home"), "DialogScraper"));
	}
}