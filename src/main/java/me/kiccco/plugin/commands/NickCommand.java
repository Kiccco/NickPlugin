package me.kiccco.plugin.commands;

import me.kiccco.plugin.NickPlayer;
import me.kiccco.plugin.NickPlugin;
import me.kiccco.plugin.managers.NickManager;
import me.kiccco.plugin.others.NickRank;
import me.kiccco.plugin.others.ServerType;
import me.kiccco.plugin.utils.PlayerUtil;
import net.minecraft.server.v1_8_R3.*;
import org.bukkit.ChatColor;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.block.BlockState;
import org.bukkit.block.Sign;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.craftbukkit.v1_8_R3.CraftWorld;
import org.bukkit.craftbukkit.v1_8_R3.entity.CraftPlayer;
import org.bukkit.craftbukkit.v1_8_R3.inventory.CraftItemStack;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

public class NickCommand implements CommandExecutor {

    private final NickManager manager;

    public NickCommand(NickManager manager)  {
        this.manager = manager;
    }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String arg, String[] args) {
        Player player = (Player) sender;
        if(!(sender instanceof Player)) {
            sender.sendMessage(ChatColor.RED + "Only players can use this comamnd!");
            return false;
        }

        if(NickPlugin.getServerType() != ServerType.LOBBY) {
            sender.sendMessage(ChatColor.RED + "You can't nick during a game!");
            return false;
        }

        if(!player.hasPermission("system.helper")) {
            player.sendMessage(ChatColor.RED + "You can't use that command!");
            return false;
        }


        if(args.length == 0) {
            if(manager.hasNickPlayer(player.getUniqueId())) {
                NickPlayer nickPlayer = NickManager.getNickPlayer(player.getUniqueId());
                if(nickPlayer.isNicked()) {
                    player.sendMessage(ChatColor.RED + "You are already nicked!. To unnick type command /nick reset");
                    return false;
                }
            }
            agreementPage(player);
            manager.addNickPlayer(player.getUniqueId());
            return false;
        }


        NickPlayer nickPlayer = NickManager.getNickPlayer(player.getUniqueId());
        if(nickPlayer == null){
            player.sendMessage(ChatColor.RED + "You are not nicked!");
            return false;
        }

        if(args[0].equals("s")) {
            startupPage(player);
            return false;
        }

        if(nickPlayer == null) {
            player.sendMessage(ChatColor.RED + "Please try again.");
            return false;
        }

        if(args[0].equals("sd")) {
            player.sendMessage(ChatColor.GREEN + "Set your nick rank to " + ChatColor.GRAY + "Default");
            nickPlayer.setRank(NickRank.DEFAULT);
            skinPage(player);
            return false;
        }

        if(args[0].equals("sv")) {
            player.sendMessage(ChatColor.GREEN + "Set your nick rank to Vip");
            nickPlayer.setRank(NickRank.VIP);
            skinPage(player);
            return false;
        }

        if(args[0].equals("svp")) {
            player.sendMessage(ChatColor.GREEN + "Set your nick rank to Vip" + ChatColor.GOLD + "+");
            nickPlayer.setRank(NickRank.VIPP);
            skinPage(player);
            return false;
        }

        if(args[0].equals("sm"))  {
            player.sendMessage(ChatColor.GREEN + "Set your nick rank to " + ChatColor.AQUA + "MVP");
            nickPlayer.setRank(NickRank.MVP);
            skinPage(player);
            return false;
        }

        if(args[0].equals("smp")) {
            player.sendMessage(ChatColor.GREEN + "Set your nick rank to " + ChatColor.AQUA + "MVP" + ChatColor.RED + "+");
            nickPlayer.setRank(NickRank.MVPP);
            skinPage(player);
            return false;
        }

        if(args[0].equals("myskin")) {
            player.sendMessage(ChatColor.GREEN + "Your skin has been set to your skin.");
            nickPlayer.setKeepSkin(true);
            namePage(player);
            return false;
        }

        if(args[0].equals("stevealex")) {
            player.sendMessage(ChatColor.GREEN + "Your skin has been set to steve/alex.");
            nickPlayer.setNickedSkin(null);
            namePage(player);
            return false;
        }

        if(args[0].equals("randomskin")) {
            manager.getRandomSkin(nickedSkin -> {
                nickPlayer.setNickedSkin(nickedSkin);
                player.sendMessage(ChatColor.GREEN + "Your skin has been set to " + nickPlayer.getNickedSkin().getName());
            });
            namePage(player);
            return false;
        }

        if(args[0].equals("reskin")) {
            nickPlayer.setNickedSkin(nickPlayer.getPreviousSkin());
            player.sendMessage(ChatColor.GREEN + "Your skin has been set to " + nickPlayer.getNickedSkin().getName());
            namePage(player);
            return false;
        }

        if(args[0].equals("en")) {
            spawnSign(player);
            return false;
        }

        if(args[0].equals("ren")) {
            nickPlayer.setName(nickPlayer.getPreviousName());
            manager.nickPlayer(nickPlayer);
            manager.insertNickPlayer(nickPlayer);
            return false;
        }

        if(args[0].equals("genn")) {
            player.sendMessage(ChatColor.YELLOW + "Generating a unique random name. Please wait..");
            randomNamePage(player);
            return false;
        }

        if(args[0].equals("stdone")) {
            player.sendMessage(ChatColor.YELLOW + "Processing request. Please wait...");
            manager.nickPlayer(nickPlayer);
            manager.insertNickPlayer(nickPlayer);
        }

        if(args[0].equals("reset")) {
            if(!manager.hasNickPlayer(player.getUniqueId())) {
                player.sendMessage(ChatColor.RED + "You aren't nicked!");
                return false;
            }
            manager.removeActiveNickPlayer(nickPlayer);
            manager.unnick(nickPlayer);
            manager.removeNickPlayer(nickPlayer);
        }

        return false;
    }


    public void agreementPage(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        net.minecraft.server.v1_8_R3.ItemStack stack = CraftItemStack.asNMSCopy(book);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", "Nick");
        tag.setString("author", "Server");
        NBTTagList pages = new NBTTagList();
        pages.add(new NBTTagString("[{text:\"Nicknames allow you to\n" +
                "play with different username to not get recognized.\n" +
                "\n" +
                "All rules still apply.\n" +
                "You can still be\n" +
                "reported and all name history is stored.\n" +
                "\n" +
                "\"}, {text: \"➤ I understand, set up my nickname\", \"underlined\": true, " +
                "hoverEvent:{action:\"show_text\", value:\"Click here to proceed\"}," +
                "clickEvent:{action: \"run_command\", value: \"/nick s\"}}]"));
        tag.set("pages", pages);
        stack.setTag(tag);
        ItemStack is = CraftItemStack.asCraftMirror(stack);
        PlayerUtil.openBook(player, is);
    }

    public void startupPage(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        net.minecraft.server.v1_8_R3.ItemStack stack = CraftItemStack.asNMSCopy(book);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", "Nick");
        tag.setString("author", "Server");
        NBTTagList pages = new NBTTagList();
        pages.add(new NBTTagString("[{text:\"Let's get you set up\n" +
                "with your nickname!\n" +
                "First, you'll need to\n" +
                "choose which \"}, {text: \"RANK\", bold:true}," +
                "{text: \"\nyou would like to be\n" +
                "shown as when nicked.\n" +
                "\n\"}," +
                "{text: \"➤\"}, {text: \" DEFAULT\n\", color: \"gray\"," +
                "hoverEvent: {action: \"show_text\", value: [{text: \"Click here to be shown as \"}, {text: \"DEFAULT\", color: \"gray\"}]}," +
                "clickEvent:{action: \"run_command\", value: \"/nick sd\"}}," +

                "{text: \"➤\", color: \"black\"}, {text: \" VIP\n\", color: \"green\"," +
                "hoverEvent: {action: \"show_text\", value: [{text: \"Click here to be shown as \"}, {text: \"VIP\", color: \"green\"}]}," +
                "clickEvent:{action: \"run_command\", value: \"/nick sv\"}}," +

                "{text: \"➤\", color: \"black\"}, {text: \" VIP\", color: \"green\"," +
                "hoverEvent: {action: \"show_text\", value: [{text: \"Click here to be shown as \"}, {text: \"VIP\", color: \"green\"}, {text: \"+\", color: \"gold\"}]}," +
                "clickEvent:{action: \"run_command\", value: \"/nick svp\"}, extra: [{text: \"+\n\", color: \"gold\"}]}," +

                "{text: \"➤\", color: \"black\"}, {text: \" MVP\n\", color: \"aqua\"," +
                "hoverEvent: {action: \"show_text\", value: [{text: \"Click here to be shown as \"}, {text: \"MVP\", color: \"aqua\"}]}," +
                "clickEvent:{action: \"run_command\", value: \"/nick sm\"}}," +

                "{text: \"➤\", color: \"black\"}, {text: \" MVP\", color: \"aqua\"," +
                "hoverEvent: {action: \"show_text\", value: [{text: \"Click here to be shown as \"}, {text: \"MVP\", color: \"aqua\"}, {text: \"+\", color: \"red\"}]}," +
                "clickEvent:{action: \"run_command\", value: \"/nick smp\"}, extra: [{text: \"+\n\", color: \"red\"}]}" +
                "]"));
        tag.set("pages", pages);
        stack.setTag(tag);
        ItemStack is = CraftItemStack.asCraftMirror(stack);
        PlayerUtil.openBook(player, is);
    }

    public void skinPage(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        net.minecraft.server.v1_8_R3.ItemStack stack = CraftItemStack.asNMSCopy(book);
        NickPlayer nickPlayer = NickManager.getNickPlayer(player.getUniqueId());
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", "Nick");
        tag.setString("author", "Server");
        NBTTagList pages = new NBTTagList();

        if(nickPlayer.getPreviousSkin() != null) {
            pages.add(new NBTTagString("[{text:\"Awesome! Now, which\n\"}," +
                    "{text: \"SKIN \", bold:true}, " +
                    "{text: \"would you like to\n" +
                    "have while nicked?\n" +
                    "\n\", color: \"reset\"}," +

                    "{text: \"➤ My normal skin\n\", " +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to use your normal skin\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick myskin\"}}," +

                    "{text: \"➤ Steve/Alex skin\n\", " +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to use Steve/Alex skin\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick stevealex\"}}," +

                    "{text: \"➤ Random skin\n\", " +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to use random skin\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick randomskin\"}}," +

                    "{text: \"➤ Reuse " + nickPlayer.getPreviousSkin().getName() + "\n\", " +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to reuse " + nickPlayer.getPreviousSkin().getName() +  " skin\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick reskin\"}}" +

                    "]"));
        } else {
            pages.add(new NBTTagString("[{text:\"Awesome! Now, which\n\"}," +
                    "{text: \"SKIN \", bold:true}, " +
                    "{text: \"would you like to\n" +
                    "have while nicked?\n" +
                    "\n\", color: \"reset\"}," +

                    "{text: \"➤ My normal skin\n\", " +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to use your normal skin\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick myskin\"}}," +

                    "{text: \"➤ Steve/Alex skin\n\", " +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to use Steve/Alex skin\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick stevealex\"}}," +

                    "{text: \"➤ Random skin\n\n\", " +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to use random skin\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick randomskin\"}}" +

                    "]"));
        }
        tag.set("pages", pages);
        stack.setTag(tag);
        ItemStack is = CraftItemStack.asCraftMirror(stack);
        PlayerUtil.openBook(player, is);
    }


    public void namePage(Player player) {
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        net.minecraft.server.v1_8_R3.ItemStack stack = CraftItemStack.asNMSCopy(book);
        NBTTagCompound tag = new NBTTagCompound();
        NickPlayer nickPlayer = NickManager.getNickPlayer(player.getUniqueId());
        tag.setString("title", "Nick");
        tag.setString("author", "Server");
        NBTTagList pages = new NBTTagList();

        if(nickPlayer.getPreviousName() != null) {
            pages.add(new NBTTagString("[{text:\"Alright, now you'll\n" +
                    "need to choose the\n\"}," +
                    "{text: \"NAME\", bold: true}, {text: \" to use \n" +
                    "\n\", color: \"reset\"}," +

                    "{text: \"➤ Enter a name\n\"," +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to enter name.\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick en\"}}," +

                    "{text: \"➤ Use a random name\n\"," +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to use random name.\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick genn\"}}," +

                    "{text: \"➤ Reuse '" + nickPlayer.getPreviousName() + "'\n\n\"," +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to use " + nickPlayer.getPreviousName() +".\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick ren\"}}," +

                    "{text:\"To go back to being\n" +
                    "your usual self, type:\n\"}," +
                    "{text: \"/nick reset\", bold: true}" +
                    "]"));
        } else {
            pages.add(new NBTTagString("[{text:\"Alright, now you'll\n" +
                    "need to choose the\n\"}," +
                    "{text: \"NAME\", bold: true}, {text: \" to use \n" +
                    "\n\", color: \"reset\"}," +

                    "{text: \"➤ Enter a name\n\"," +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to enter name.\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick en\"}}," +

                    "{text: \"➤ Use a random name\n\n\"," +
                    "hoverEvent:{action:\"show_text\", value:\"Click here to use random name.\"}," +
                    "clickEvent:{action: \"run_command\", value: \"/nick genn\"}}," +

                    "{text:\"To go back to being\n" +
                    "your usual self, type:\n\"}," +
                    "{text: \"/nick reset\", bold: true}" +
                    "]"));
        }

        tag.set("pages", pages);
        stack.setTag(tag);
        ItemStack is = CraftItemStack.asCraftMirror(stack);
        PlayerUtil.openBook(player, is);
    }

    public void spawnSign(Player player)  {
        NickPlayer nickPlayer = manager.getNickPlayer(player.getUniqueId());
        Location location = new Location(player.getWorld(), player.getLocation().getX(), 5, player.getLocation().getZ());
        location.getWorld().getBlockAt(location).setType(Material.SIGN_POST);

        BlockState blockState = location.getWorld().getBlockAt(location).getState();
        Sign sign = (Sign) blockState;
        sign.setLine(1, "^^^^^^^^^^^^^^^");
        sign.setLine(2, "Enter your");
        sign.setLine(3, "username here");
        sign.update();
        nickPlayer.setSign(location);
        TileEntitySign tileSign = (TileEntitySign) ((CraftWorld) location.getWorld()).getTileEntityAt(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        tileSign.a(((CraftPlayer) player).getHandle());
        tileSign.isEditable = true;
        BlockPosition blockPosition = new BlockPosition(location.getBlockX(), location.getBlockY(), location.getBlockZ());
        new BukkitRunnable() {
            @Override
            public void run() {
                PacketPlayOutOpenSignEditor signEditor = new PacketPlayOutOpenSignEditor(blockPosition);
                PlayerUtil.sendPacket(player, signEditor);
            }
        }.runTaskLater(NickPlugin.getInstance(), 4);


    }

    public void randomNamePage(Player player) {
        NickPlayer nickPlayer = manager.getNickPlayer(player.getUniqueId());
        String name = manager.getRandomName();
        nickPlayer.setName(name);
        ItemStack book = new ItemStack(Material.WRITTEN_BOOK);
        net.minecraft.server.v1_8_R3.ItemStack stack = CraftItemStack.asNMSCopy(book);
        NBTTagCompound tag = new NBTTagCompound();
        tag.setString("title", "Nick");
        tag.setString("author", "Kiccco");
        NBTTagList pages = new NBTTagList();
        pages.add(new NBTTagString("[{text:\"We've generated a\n" +
                "random username for\n" +
                "you:\n\"}," +
                "{text: \"" + name + "\n\n\", bold: true}," +
                "{text: \"       \", color: \"reset\"}," +
                "{text: \"USE NAME\n\", underlined: true,bold: true, color: \"green\"," +
                "hoverEvent:{action:\"show_text\", value:\"Click here to use this name.\"}," +
                "clickEvent:{action: \"run_command\", value: \"/nick stdone\"}}," +
                "{text: \"      \", color: \"reset\"}," +
                "{text: \"TRY AGAIN\n\", underlined: true, bold: true,color: \"red\"," +
                "hoverEvent:{action:\"show_text\", value:\"Click here to try again.\"}," +
                "clickEvent:{action: \"run_command\", value: \"/nick genn\"}}" +



                "]"));
        tag.set("pages", pages);
        stack.setTag(tag);
        ItemStack is = CraftItemStack.asCraftMirror(stack);
        PlayerUtil.openBook(player, is);
    }


}
