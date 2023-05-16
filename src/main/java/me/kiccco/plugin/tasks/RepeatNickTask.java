package me.kiccco.plugin.tasks;

import me.kiccco.plugin.NickPlayer;
import me.kiccco.plugin.utils.PlayerUtil;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.scheduler.BukkitRunnable;

public class RepeatNickTask extends BukkitRunnable {

    private final NickPlayer nickPlayer;
    private final Player player;

    public RepeatNickTask(NickPlayer nickPlayer) {
        this.nickPlayer = nickPlayer;
        this.player = Bukkit.getPlayer(nickPlayer.getUuid());
    }

    @Override
    public void run() {
        if(!player.isOnline()) cancel();
        if(!nickPlayer.isNicked()) {
            PlayerUtil.sendActionBar(player, "");
            cancel();
            return;
        }
        PlayerUtil.sendActionBar(player, "You are currently §cNICKED§f");
    }
}
