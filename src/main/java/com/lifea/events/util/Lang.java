package com.lifea.events.util;

public final class Lang {
   public static final String BRAND = MessageUtil.smallCaps("LifeaEvents");
   public static final String HOST = MessageUtil.smallCaps("Host");
   public static final String MINI_GAME = MessageUtil.smallCaps("Mini Game");
   public static final String CAPACITY = MessageUtil.smallCaps("Capacity");
   public static final String STARTING_IN = MessageUtil.smallCaps("Starting In");
   public static final String CLICK_HERE_TO_JOIN_EVENT = MessageUtil.smallCaps("[ Click Here To Join Event ]");
   public static final String EVENT_STARTING_SOON = MessageUtil.smallCaps("Starting Soon!");
   public static final String HOST_EVENT = MessageUtil.smallCaps("Host Event");
   public static final String ARENA_SETUP = MessageUtil.smallCaps("Arena Setup");
   public static final String START_SETUP_TITLE_PREFIX = "";
   public static final String SELECT_MINIGAME = MessageUtil.smallCaps("Select A Minigame");
   public static final String SELECTED = MessageUtil.smallCaps("Selected");
   public static final String NOT_SELECTED = MessageUtil.smallCaps("Not Selected");
   public static final String MIN_PLAYERS = MessageUtil.smallCaps("Min Players");
   public static final String MAX_PLAYERS = MessageUtil.smallCaps("Max Players");
   public static final String CONFIRM_AND_ANNOUNCE = MessageUtil.smallCaps("Confirm & Announce Host");
   public static final String SELECT_A_MINIGAME_FIRST = MessageUtil.smallCaps("Select A Minigame First");
   public static final String CLICK_TO_ANNOUNCE = MessageUtil.smallCaps("Click To Announce This Event!");
   public static final String FULLY_CONFIGURED = MessageUtil.smallCaps("✔ Fully Configured");
   public static final String INCOMPLETE_SETUP = MessageUtil.smallCaps("⚠ Incomplete Setup");
   public static final String CLICK_TO_CONFIGURE = MessageUtil.smallCaps("Click To Configure This Arena");
   public static final String START_SETUP = MessageUtil.smallCaps("✔ Start Setup");
   public static final String START_SETUP_DESC = MessageUtil.smallCaps("Click to begin the guided setup wand for this minigame.");
   public static final String BACK = MessageUtil.smallCaps("← Back");
   public static final String STEP_PRE_LOBBY = MessageUtil.smallCaps("Pre-Lobby Spawn");
   public static final String STEP_CORNER_1 = MessageUtil.smallCaps("Arena Corner 1");
   public static final String STEP_CORNER_2 = MessageUtil.smallCaps("Arena Corner 2");
   public static final String STEP_MAIN_SPAWN = MessageUtil.smallCaps("Main Spawn");
   public static final String STEP_RED_SPAWN = MessageUtil.smallCaps("Red Spawn");
   public static final String STEP_BLUE_SPAWN = MessageUtil.smallCaps("Blue Spawn");
   public static final String STEP_HIDERS_SPAWN = MessageUtil.smallCaps("Hiders Spawn");
   public static final String STEP_SEEKERS_SPAWN = MessageUtil.smallCaps("Seekers Spawn");
   public static final String STEP_START_LINE_CORNER_1 = MessageUtil.smallCaps("Start Line Corner 1");
   public static final String STEP_START_LINE_CORNER_2 = MessageUtil.smallCaps("Start Line Corner 2");
   public static final String STEP_FINISH_LINE_CORNER_1 = MessageUtil.smallCaps("Finish Line Corner 1");
   public static final String STEP_FINISH_LINE_CORNER_2 = MessageUtil.smallCaps("Finish Line Corner 2");
   public static final String SETUP_WAND_NAME = MessageUtil.smallCaps("Arena Setup Wand");
   public static final String LEFT_CLICK_TO_SET = MessageUtil.smallCaps("Left-click anywhere to set this to your current location.");
   public static final String SETUP_WAND_LORE = MessageUtil.smallCaps("Left or right-click anywhere - follow the prompts.");
   public static final String SETUP_COMPLETE = MessageUtil.smallCaps("Setup Complete!");

   private Lang() {
   }

   public static String stepLabel(String key) {
      return switch (key) {
         case "PRE_LOBBY" -> STEP_PRE_LOBBY;
         case "CORNER_1" -> STEP_CORNER_1;
         case "CORNER_2" -> STEP_CORNER_2;
         case "MAIN_SPAWN" -> STEP_MAIN_SPAWN;
         case "RED_SPAWN" -> STEP_RED_SPAWN;
         case "BLUE_SPAWN" -> STEP_BLUE_SPAWN;
         case "HIDERS_SPAWN" -> STEP_HIDERS_SPAWN;
         case "SEEKERS_SPAWN" -> STEP_SEEKERS_SPAWN;
         case "START_LINE_CORNER_1" -> STEP_START_LINE_CORNER_1;
         case "START_LINE_CORNER_2" -> STEP_START_LINE_CORNER_2;
         case "FINISH_LINE_CORNER_1" -> STEP_FINISH_LINE_CORNER_1;
         case "FINISH_LINE_CORNER_2" -> STEP_FINISH_LINE_CORNER_2;
         default -> MessageUtil.smallCaps(key.replace("_", " "));
      };
   }
}
