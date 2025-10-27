package com.minkang.ultimate.voteplus;

import org.bukkit.ChatColor;
import org.bukkit.command.*;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AutoNoticeCommand implements CommandExecutor, TabCompleter {
    private final AutoNoticeManager manager;

    public AutoNoticeCommand(AutoNoticeManager manager) {
        this.manager = manager;
    }

    private String c(String m) { return ChatColor.translateAlternateColorCodes('&', m); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!command.getName().equals("자동공지")) return false;
        if (!sender.hasPermission("ultimatevoteplus.autonotice") && !sender.isOp()) {
            sender.sendMessage(c("&c권한이 없습니다."));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(c("&a/자동공지 추가 <번호> <내용>"));
            sender.sendMessage(c("&a/자동공지 삭제 <번호>"));
            sender.sendMessage(c("&a/자동공지 시간 <번호> <초>"));
            sender.sendMessage(c("&7현재 등록된 공지: &f" + manager.getIds()));
            return true;
        }

        String sub = args[0];
        if (sub.equalsIgnoreCase("추가") && args.length >= 3) {
            String id = args[1];
            String text = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
            manager.setText(id, text);
            sender.sendMessage(c("&a공지 #" + id + " 내용이 등록/수정되었습니다. &7(시간은 별도 설정 필요: /자동공지 시간 " + id + " <초>)"));
            manager.start(); // 재시작하여 즉시 반영
            return true;
        } else if (sub.equalsIgnoreCase("삭제") && args.length == 2) {
            String id = args[1];
            manager.remove(id);
            sender.sendMessage(c("&c공지 #" + id + " 가 삭제되었습니다."));
            manager.start();
            return true;
        } else if (sub.equalsIgnoreCase("시간") && args.length == 3) {
            String id = args[1];
            int sec;
            try {
                sec = Integer.parseInt(args[2]);
            } catch (NumberFormatException e) {
                sender.sendMessage(c("&c초는 숫자로 입력하세요."));
                return true;
            }
            manager.setSeconds(id, sec);
            sender.sendMessage(c("&a공지 #" + id + " 의 간격이 &e" + sec + "초 &a로 설정되었습니다."));
            manager.start();
            return true;
        }

        sender.sendMessage(c("&c사용법: /자동공지 추가 <번호> <내용> | 삭제 <번호> | 시간 <번호> <초>"));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!command.getName().equals("자동공지")) return Collections.emptyList();
        if (args.length == 1) {
            return Arrays.asList("추가", "삭제", "시간");
        } else if (args.length == 2) {
            return manager.getIds();
        } else if (args.length == 3 && "시간".equalsIgnoreCase(args[0])) {
            return Arrays.asList("30", "60", "90", "120");
        }
        return Collections.emptyList();
    }
}
