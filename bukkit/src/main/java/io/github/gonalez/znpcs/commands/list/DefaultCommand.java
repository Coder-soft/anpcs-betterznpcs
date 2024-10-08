package io.github.gonalez.znpcs.commands.list;

import static io.github.gonalez.znpcs.ZNPConfigUtils.getConfig;
import static io.github.gonalez.znpcs.utility.Utils.toColor;

import com.google.common.base.Joiner;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.Iterables;
import com.google.common.primitives.Doubles;
import com.google.common.primitives.Ints;
import io.github.gonalez.znpcs.ServersNPC;
import io.github.gonalez.znpcs.ZNPConfigUtils;
import io.github.gonalez.znpcs.commands.Command;
import io.github.gonalez.znpcs.commands.CommandInformation;
import io.github.gonalez.znpcs.commands.CommandSenderUtil;
import io.github.gonalez.znpcs.commands.list.inventory.ConversationGUI;
import io.github.gonalez.znpcs.configuration.ConversationsConfiguration;
import io.github.gonalez.znpcs.configuration.DataConfiguration;
import io.github.gonalez.znpcs.configuration.MessagesConfiguration;
import io.github.gonalez.znpcs.npc.FunctionContext;
import io.github.gonalez.znpcs.npc.FunctionFactory;
import io.github.gonalez.znpcs.npc.ItemSlot;
import io.github.gonalez.znpcs.npc.NPC;
import io.github.gonalez.znpcs.npc.NPCAction;
import io.github.gonalez.znpcs.npc.NPCFunction;
import io.github.gonalez.znpcs.npc.NPCModel;
import io.github.gonalez.znpcs.npc.NPCPath;
import io.github.gonalez.znpcs.npc.NPCSkin;
import io.github.gonalez.znpcs.npc.NPCType;
import io.github.gonalez.znpcs.npc.conversation.Conversation;
import io.github.gonalez.znpcs.npc.conversation.ConversationModel;
import io.github.gonalez.znpcs.skin.SkinFetcherResult;
import io.github.gonalez.znpcs.user.ZUser;
import io.github.gonalez.znpcs.utility.PlaceholderUtils;
import io.github.gonalez.znpcs.utility.Utils;
import io.github.gonalez.znpcs.utility.location.ZLocation;
import net.md_5.bungee.api.chat.BaseComponent;
import net.md_5.bungee.api.chat.ClickEvent;
import net.md_5.bungee.api.chat.ComponentBuilder;
import net.md_5.bungee.api.chat.HoverEvent;
import net.md_5.bungee.api.chat.TextComponent;
import org.bukkit.Bukkit;
import org.bukkit.ChatColor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import javax.annotation.Nullable;
import java.io.File;
import java.lang.reflect.Method;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Objects;

public class DefaultCommand extends Command {
  private static final Joiner SPACE_JOINER = Joiner.on(" ");

  private static final SkinFunction DO_APPLY_SKIN = new SkinFunction() {
    @Override
    public void apply(CommandSender commandSender, NPC npc, String skinName, @Nullable SkinFetcherResult result) {
      NPCSkin.forName(skinName, (paramArrayOfString, throwable) -> {
        if (throwable != null) {
          Utils.sendMessage(commandSender, getConfig(MessagesConfiguration.class).cantGetSkin, skinName);
        } else {
          npc.changeSkin(NPCSkin.forValues(paramArrayOfString));
          Utils.sendMessage(commandSender, getConfig(MessagesConfiguration.class).getSkin, skinName);
        }
        if (result != null) {
          result.onDone(paramArrayOfString, throwable);
        }
      });
    }
  };

  private final Path path;

  public DefaultCommand(Path path) {
    super("anpcs");
    this.path = path;
  }
  
  @CommandInformation(arguments = {}, name = "", permission = "")
  public void defaultCommand(CommandSender sender, ImmutableList<String> args) {
    sender.sendMessage(toColor("&6&m------------------------------------------"));
    sender.sendMessage(toColor("&b&lANPCS &8» &7ArcaneNetwork"));
    sender.sendMessage(toColor("&6http://coder-soft.github.io/arcanecore"));
    Objects.requireNonNull(sender);
    getCommands().forEach(commandInformation -> CommandSenderUtil.sendMessage(sender, commandInformation));
    sender.sendMessage(ChatColor.DARK_GRAY + "Hover over the commands to see help for the command.");
    sender.sendMessage(toColor("&6&m------------------------------------------"));
  }
  
  @CommandInformation(arguments = {"id", "type", "name"}, name = "create", permission = "anpcs.cmd.create", help = {" &f&l* &e/anpcs create <npc_id> PLAYER boyka"})
  public void createNPC(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 3) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    }
    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    }
    boolean foundNPC = getConfig(DataConfiguration.class).npcList.stream().anyMatch(npc -> (npc.getId() == id));
    if (foundNPC) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcFound);
      return;
    }
    NPCType npcType = NPCType.valueOf(args.get(1).toUpperCase());
    String name = args.get(2).trim();
    if (name.length() < 3 || name.length() > 16) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNameLength);
      return;
    }
    NPC npc = ServersNPC.createNPC(id, npcType, ((Player)sender).getLocation(), name);
    Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
    if (npcType == NPCType.PLAYER) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).fetchingSkin, name);
      DO_APPLY_SKIN.apply(sender, npc, name, null);

      if (args.size() >= 3) {
        sender.sendMessage("");
      }
    }
  }

  @CommandInformation(arguments = {"id"}, name = "delete", permission = "anpcs.cmd.delete", help = {" &f&l* &e/anpcs delete <npc_id>"})
  public void deleteNPC(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 1) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    }
    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    }
    NPC foundNPC = NPC.find(id);
    if (foundNPC == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
      return;
    }
    ServersNPC.deleteNPC(id);
    Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
  }

  @CommandInformation(arguments = {}, name = "list", permission = "anpcs.cmd.list")
  public void list(CommandSender sender, ImmutableList<String> args) {
    if (getConfig(DataConfiguration.class).npcList.isEmpty()) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).noNpcFound);
    } else {
      sender.sendMessage(ChatColor.DARK_GREEN + "NPC list:");
      for (NPCModel npcModel : getConfig(DataConfiguration.class).npcList) {
        List<BaseComponent> parts = new ArrayList<>();

        String message = "- "
            + npcModel.getId() + " "
            + npcModel.getHologramLines().toString()
            + " (" + npcModel.getLocation().getWorldName() + " "
            + (int)npcModel.getLocation().getX() + " "
            + (int)npcModel.getLocation().getY() + " "
            + (int)npcModel.getLocation().getZ() + ") ";
        TextComponent textComponent = new TextComponent(message);
        textComponent.setColor(net.md_5.bungee.api.ChatColor.GREEN);
        parts.add(textComponent);

        TextComponent textComponent2 = new TextComponent("[TELEPORT]");
        textComponent2.setBold(true); textComponent2.setColor(net.md_5.bungee.api.ChatColor.DARK_GREEN);
        textComponent2.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("Click to teleport this npc!")
                .color(net.md_5.bungee.api.ChatColor.GREEN).create()));
        textComponent2.setClickEvent(
            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/anpcs teleport " + npcModel.getId()));
        parts.add(textComponent2);
        parts.add(new TextComponent(" "));

        TextComponent textComponent3 = new TextComponent("[DELETE]");
        textComponent3.setBold(true);
        textComponent3.setColor(net.md_5.bungee.api.ChatColor.DARK_RED);
        textComponent3.setHoverEvent(new HoverEvent(HoverEvent.Action.SHOW_TEXT,
            new ComponentBuilder("Click to delete this npc!")
                .color(net.md_5.bungee.api.ChatColor.RED).create()));
        textComponent3.setClickEvent(
            new ClickEvent(ClickEvent.Action.RUN_COMMAND, "/anpcs delete " + npcModel.getId()));
        parts.add(textComponent3);

        ((Player)sender).spigot().sendMessage(parts.toArray(new BaseComponent[0]));
      } 
    } 
  }
  
  @CommandInformation(arguments = {"id", "skin"},
      name = "skin", permission = "anpcs.cmd.skin",
      help = {
      " &f&l* &e/anpcs skin <npc_id> Notch",
      " &f&l* &e/anpcs skin <npc_id> %someplugin_balance_top_1% 60",
      "",
      "&7If &erefreshSkinDuration &7is present in the command arguments",
      "&7the skin will be 'refreshed' periodically every that duration, ",
      "&7in seconds. The skin will be refreshed using the skin name, ",
      "&7placeholders are supported, example: &e%someplugin_balance_top_1%"})
  public void setSkin(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 1) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    } 
    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    } 
    NPC foundNPC = NPC.find(id);
    if (foundNPC == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
      return;
    }

    String skin = args.get(1).trim();
    foundNPC.getNpcPojo().setSkinName(skin);

    Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).fetchingSkin, skin);
    DO_APPLY_SKIN.apply(sender, foundNPC, PlaceholderUtils.formatPlaceholders(skin), new SkinFetcherResult() {
      @Override
      public void onDone(String[] paramArrayOfString, Throwable paramThrowable) {
        if (args.size() == 3) {
          Integer refreshSkinDuration = Ints.tryParse(args.get(2));
          if (refreshSkinDuration != null) {
            foundNPC.getNpcPojo().setRefreshSkinDuration(refreshSkinDuration);
            sender.sendMessage(ChatColor.GREEN + "The skin will refresh every: " +
                ChatColor.YELLOW +  refreshSkinDuration + ChatColor.GREEN + " seconds.");
          }
        } else {
          foundNPC.getNpcPojo().setRefreshSkinDuration(0);
        }
      }
    });
  }
  
  @CommandInformation(arguments = {"id", "slot"}, name = "equip", permission = "anpcs.cmd.equip", help = {" &f&l* &e/anpcs equip <npc_id> [HAND,OFFHAND,HELMET,CHESTPLATE,LEGGINGS,BOOTS]", "&8(You need to have the item in your hand.)"})
  public void equip(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 2) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    } 
    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    } 
    NPC foundNPC = NPC.find(id);
    if (foundNPC == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
      return;
    } 
    foundNPC.getNpcPojo().getNpcEquip().put(
        ItemSlot.valueOf(args.get(1).toUpperCase()),
            ((Player)sender).getInventory().getItemInHand());
    foundNPC.getPackets().flushCache("equipPackets");
    Objects.requireNonNull(foundNPC);
    foundNPC.getViewers().forEach(foundNPC::sendEquipPackets);
    Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
  }
  
  @CommandInformation(arguments = {"id", "set", "add", "list", "remove"}, name = "lines",
      permission = "anpcs.cmd.lines",
      help = {
      "&f&l* &e/anpcs lines <npc_id> add Example Line",
      "&f&l* &e/anpcs lines <npc_id> remove 0",
      "&f&l* &e/anpcs lines <npc_id> set First Second Third-Space"
  })
  public void changeLines(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 2) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    }

    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    }

    NPC foundNPC = NPC.find(id);
    if (foundNPC == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
      return;
    }

    if (args.get(1).equals("clear")) {
      foundNPC.getNpcPojo().getHologramLines().clear();
    } else {
      List<String> toAdd = new ArrayList<>(args.subList(2, args.size()));
      switch (args.get(1)) {
        case "set":
          Collections.reverse(toAdd);
          foundNPC.getNpcPojo().setHologramLines(toAdd);
          break;
        case "add":
          foundNPC.getNpcPojo().getHologramLines().add(SPACE_JOINER.join(toAdd));
          break;
        case "list":
          sender.sendMessage(ChatColor.YELLOW + "NPC Lines:");
          for (int i = 0; i < foundNPC.getNpcPojo().getHologramLines().size(); i++) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',
                String.format("&a%d &7- " + foundNPC.getNpcPojo().getHologramLines().get(i), i)));
          }
          break;
        case "remove":
          Integer num = Ints.tryParse(args.get(2));
          if (num == null) {
            Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
            return;
          }
          if (num >= foundNPC.getNpcPojo().getHologramLines().size()) {
            Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).noLineFound);
            return;
          }
          foundNPC.getNpcPojo().getHologramLines().remove(
              foundNPC.getNpcPojo().getHologramLines().get(num));
          break;
        default:
          break;
      }
    }
    foundNPC.getHologram().createHologram();
    Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
  }
  
  @CommandInformation(arguments = {"id"}, name = "move", permission = "anpcs.cmd.move", help = {" &f&l* &e/anpcs move <npc_id>"})
  public void move(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 1) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    } 
    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    } 
    NPC foundNPC = NPC.find(id);
    if (foundNPC == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
      return;
    }

    foundNPC.getNpcPojo().setLocation(new ZLocation(((Player)sender).getLocation()));
    foundNPC.changeType(foundNPC.getNpcPojo().getNpcType());

    Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
  }
  
  @CommandInformation(arguments = {"id", "type"}, name = "type", permission = "anpcs.cmd.type", help = {" &f&l* &e/anpcs type <npc_id> ZOMBIE"})
  public void changeType(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 2) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    } 
    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    } 
    NPC foundNPC = NPC.find(id);
    if (foundNPC == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
      return;
    } 
    NPCType npcType = NPCType.valueOf(args.get(1).toUpperCase());
    if (npcType != NPCType.PLAYER && npcType.getConstructor() == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).unsupportedEntity);
      return;
    } 
    foundNPC.changeType(npcType);
    Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
  }
  
  @CommandInformation(arguments = {"add", "remove", "cooldown", "list"}, name = "action", isMultiple = true, permission = "anpcs.cmd.action",
      help = {
      " &f&l* &e/anpcs action <npc_id> add SERVER skywars",
          " &f&l* &e/anpcs action <npc_id> add CMD spawn",
          " &f&l* &e/anpcs action <npc_id> remove <action_id>",
          " &f&l* &e/anpcs action <npc_id> cooldown <action_id> <delay_in_seconds>", " &f&l* &e/anpcs action list <npc_id>"})
  public void action(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 1) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    }

    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    }
    NPC foundNPC = NPC.find(id);
    if (foundNPC == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
      return;
    }

    List<String> split = args.subList(1, args.size());
    switch (args.get(1)) {
      case "add":
        if (split.isEmpty()) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).actionAddIncorrectUsage);
          return;
        }
        foundNPC.getNpcPojo().getClickActions().add(new NPCAction(split.get(1).toUpperCase(), SPACE_JOINER.join(Iterables.skip(split, 2))));
        Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
        break;
      case "remove":
        Integer actionId = Ints.tryParse(split.get(1));
        if (actionId == null) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
        } else {
          if (actionId >= foundNPC.getNpcPojo().getClickActions().size()) {
            Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).noActionFound);
            return;
          }
          foundNPC.getNpcPojo().getClickActions().remove(actionId.intValue());
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
        }
        break;
      case "cooldown":
        Integer actionDelay = Ints.tryParse(split.get(2));
        actionId = Ints.tryParse(split.get(1));
        if (actionId == null || id == null || actionDelay == null) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
        } else {
          if (actionId >= foundNPC.getNpcPojo().getClickActions().size()) {
            Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).noActionFound);
            return;
          }
          foundNPC.getNpcPojo().getClickActions().get(actionId).setDelay(actionDelay);
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
        }
        break;
      case "list":
        if (foundNPC.getNpcPojo().getClickActions().isEmpty()) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).noActionFound);
        } else {
          foundNPC.getNpcPojo().getClickActions().forEach(s -> sender.sendMessage("&8(&a" + foundNPC.getNpcPojo().getClickActions().indexOf(s) + "&8) &6" + s.toString()));
        }
        break;
      default:
        break;
    }
  }
  
  @CommandInformation(arguments = {"id", "type", "valueOptional"}, name = "toggle", permission = "anpcs.cmd.toggle", help = {" &f&l* &e/anpcs toggle <npc_id> look"})
  public void toggle(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 2) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    } 
    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    } 
    NPC foundNPC = NPC.find(id);
    if (foundNPC == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
      return;
    } 
    NPCFunction npcFunction = FunctionFactory.findFunctionForName(args.get(1));
    if (npcFunction.getName().equalsIgnoreCase("glow")) {
      npcFunction.doRunFunction(foundNPC, new FunctionContext.ContextWithValue(foundNPC, args.get(2)));
    } else {
      npcFunction.doRunFunction(foundNPC, new FunctionContext.DefaultContext(foundNPC));
    } 
    Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
  }
  
  @CommandInformation(arguments = {"id", "customizeValues"}, name = "customize", permission = "anpcs.cmd.customize", help = {" &f&l* &e/anpcs customize <npc_id> <customization>"})
  public void customize(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 2) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    } 
    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    } 
    NPC foundNPC = NPC.find(id);
    if (foundNPC == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
      return;
    } 
    NPCType npcType = foundNPC.getNpcPojo().getNpcType();
    List<String> customizeOptions = args.subList(1, args.size());
    String methodName = customizeOptions.get(0);
    if (npcType.getCustomizationLoader().contains(methodName)) {
      Method method = npcType.getCustomizationLoader().getMethods().get(methodName);
      Iterable<String> split = Iterables.skip(customizeOptions, 1);
      if (Iterables.size(split) < (method.getParameterTypes()).length) {
        Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).tooFewArguments);
        return;
      } 
      String[] values = Iterables.toArray(split, String.class);
      npcType.updateCustomization(foundNPC, methodName, values);
      foundNPC.getNpcPojo().getCustomizationMap().put(methodName, values);
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
    } else {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).methodNotFound);
      for (Map.Entry<String, Method> method : npcType.getCustomizationLoader().getMethods().entrySet())
        sender.sendMessage(ChatColor.YELLOW + method.getKey() + " " + SPACE_JOINER.join(method.getValue().getParameterTypes()));
    } 
  }
  
  @CommandInformation(arguments = {"set", "create", "exit", "path", "list"}, name = "path", isMultiple = true, permission = "anpcs.cmd.path", help = {" &f&l* &e/anpcs path create name", " &f&l* &e/anpcs path set <npc_id> name"})
  public void path(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 1) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    }

    ZUser znpcUser = ZUser.find(((Player) sender).getPlayer());
    if (znpcUser == null)
      return;

    switch (args.get(0)) {
      case "set":
        Integer id = Ints.tryParse(args.get(1));
        if (id == null) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
          return;
        }

        NPC foundNPC = NPC.find(id);
        if (foundNPC == null) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
          return;
        }
        foundNPC.setPath(NPCPath.AbstractTypeWriter.find(args.get(2)));
        Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
        break;
      case "create":
        String pathName = args.get(1);
        if (pathName.length() < 3 || pathName.length() > 16) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNameLength);
          return;
        }
        if (NPCPath.AbstractTypeWriter.find(pathName) != null) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).pathFound);
          return;
        }
        if (znpcUser.isHasPath()) {
          sender.sendMessage(ChatColor.RED + "You already have a path creator active, to remove it use /anpcs path exit.");
          return;
        }

        File file = path.resolve(pathName + ServersNPC.PATH_EXTENSION).toFile();
        NPCPath.AbstractTypeWriter.forCreation(file, znpcUser, NPCPath.AbstractTypeWriter.TypeWriter.MOVEMENT);
        Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).pathStart);
        break;
      case "exit":
        znpcUser.setHasPath(false);
        Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).exitPath);
        break;
      case "list":
        if (NPCPath.AbstractTypeWriter.getPaths().isEmpty()) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).noPathFound);
        } else {
          NPCPath.AbstractTypeWriter.getPaths().forEach(path -> sender.sendMessage(ChatColor.GREEN + path.getName()));
        }
        break;
      default:
        break;
    }
  }
  
  @CommandInformation(arguments = {"id"}, name = "teleport", permission = "anpcs.cmd.teleport", help = {" &f&l* &e/anpcs teleport <npc_id>"})
  public void teleport(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 1) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    } 
    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    } 
    NPC foundNPC = NPC.find(id);
    if (foundNPC == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
      return;
    } 
    ((Player)sender).teleport(foundNPC.getLocation());
  }
  
  @CommandInformation(arguments = {"id", "height"}, name = "height", permission = "anpcs.cmd.height", help = {" &f&l* &e/anpcs height <npc_id> 2", "&8Add more height to the hologram of the npc"})
  public void changeHologramHeight(CommandSender sender, ImmutableList<String> args) {
    if (args.size() < 2) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    } 
    Integer id = Ints.tryParse(args.get(0));
    if (id == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    } 
    NPC foundNPC = NPC.find(id);
    if (foundNPC == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
      return;
    } 
    Double givenHeight = Doubles.tryParse(args.get(1));
    if (givenHeight == null) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
      return;
    } 
    foundNPC.getNpcPojo().setHologramHeight(givenHeight);
    foundNPC.getHologram().createHologram();
    Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
  }
  
  @CommandInformation(arguments = {"create", "remove", "gui", "set"}, name = "conversation", isMultiple = true, permission = "anpcs.cmd.conversation", help = {" &f&l* &e/anpcs conversation create first", " &f&l* &e/anpcs conversation remove first", " &f&l* &e/anpcs conversation set <npc_id> first [CLICK:RADIUS]", " &f&l* &e/anpcs conversation gui &8(&7Open a gui to manage the conversations&8)", "&8RADIUS: &7it is activated when the player is near the npc", "&8CLICK: &7it is activated when the player interacts with the npc"})
  public void conversations(CommandSender sender, ImmutableList<String> args) {
    if (args.isEmpty()) {
      Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).incorrectUsage);
      return;
    }

    switch (args.get(0)) {
      case "create": {
        String conversationName = args.get(1);
        if (Conversation.exists(conversationName)) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).conversationFound);
          return;
        }
        if (conversationName.length() < 3 || conversationName.length() > 16) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNameLength);
          return;
        }
        getConfig(ConversationsConfiguration.class).conversationList.add(new Conversation(conversationName));
        Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
        break;
      }
      case "remove":
        String conversationName = args.get(1);
        if (!Conversation.exists(conversationName)) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).noConversationFound);
          return;
        }
        getConfig(ConversationsConfiguration.class).conversationList.remove(Conversation.forName(conversationName));
        Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
        break;
      case "gui":
        ((Player)sender).openInventory((new ConversationGUI(((Player) sender))).build());
        break;
      case "set":
        List<String> split = args.subList(1, args.size());
        if (split.size() < 2) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).conversationSetIncorrectUsage);
          return;
        }
        Integer id = Ints.tryParse(split.get(0));
        if (id == null) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).invalidNumber);
          return;
        }
        NPC foundNPC = NPC.find(id);
        if (foundNPC == null) {
          Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).npcNotFound);
          return;
        }
        conversationName = split.get(1);
        if (Conversation.exists(conversationName)) {
          foundNPC.getNpcPojo().setConversation(new ConversationModel(conversationName, (split.size() > 1) ? split.get(2) : "CLICK"));
        } else {
          foundNPC.getNpcPojo().setConversation(null);
        }
        Utils.sendMessage(sender, getConfig(MessagesConfiguration.class).success);
        break;
      default:
        break;
    }
  }
  
  interface SkinFunction {
    void apply(CommandSender commandSender, NPC npc, String skinName, SkinFetcherResult result);
  }
}
