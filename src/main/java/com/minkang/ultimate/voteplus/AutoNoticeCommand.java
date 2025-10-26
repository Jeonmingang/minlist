package com.minkang.ultimate.voteplus;

import org.bukkit.ChatColor;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.command.TabCompleter;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

public class AutoNoticeCommand implements CommandExecutor, TabCompleter {
    private final AutoNoticeManager manager;

    public AutoNoticeCommand(AutoNoticeManager manager) {
        this.manager = manager;
    }

    private String color(String s) { return ChatColor.translateAlternateColorCodes('&', s); }

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args){
        if (!(command.getName().equals("마인리스트") || command.getName().equals("추천"))) return false;
        if (!sender.hasPermission("uvp.admin")) {
            sender.sendMessage(ChatColor.translateAlternateColorCodes('&',"&c권한이 없습니다. (uvp.admin)"));
            return true;
        }

        if (args.length == 0) {
            sender.sendMessage(color("&a/자동공지 &7[추가|삭제|목록|시간|기본시간|켜기|끄기]"));
            sender.sendMessage(color("&7 - &f/자동공지 목록"));
            sender.sendMessage(color("&7 - &f/자동공지 추가 <번호> <내용>"));
            sender.sendMessage(color("&7 - &f/자동공지 삭제 <번호>"));
            sender.sendMessage(color("&7 - &f/자동공지 시간 <번호> <초>"));
            sender.sendMessage(color("&7 - &f/자동공지 기본시간 <초>"));
            sender.sendMessage(color("&7 - &f/자동공지 켜기 | /자동공지 끄기"));
            return true;
        }
        String sub = args[0];

        if (sub.equalsIgnoreCase("목록")) {
            manager.sendList(sender);
            return true;
        }
        if (sub.equalsIgnoreCase("추가")) {
            if (args.length < 3) { sender.sendMessage(color("&c사용법: /자동공지 추가 <번호> <내용>")); return true; }
            try {
                int id = Integer.parseInt(args[1]);
                String content = String.join(" ", Arrays.copyOfRange(args, 2, args.length));
                manager.addMessage(id, content);
                sender.sendMessage(color("&a추가 완료: &e#" + id));
            } catch (NumberFormatException ex) {
                sender.sendMessage(color("&c번호는 정수여야 합니다."));
            }
            return true;
        }
        if (sub.equalsIgnoreCase("삭제")) {
            if (args.length < 2) { sender.sendMessage(color("&c사용법: /자동공지 삭제 <번호>")); return true; }
            try {
                int id = Integer.parseInt(args[1]);
                boolean ok = manager.removeMessage(id);
                sender.sendMessage(color(ok ? "&a삭제 완료: &e#" + id : "&c해당 번호가 없습니다: #" + id));
            } catch (NumberFormatException ex) {
                sender.sendMessage(color("&c번호는 정수여야 합니다."));
            }
            return true;
        }
        if (sub.equalsIgnoreCase("시간")) {
            if (args.length < 3) { sender.sendMessage(color("&c사용법: /자동공지 시간 <번호> <초> (최소 5초)")); return true; }
            try {
                int id = Integer.parseInt(args[1]);
                int sec = Integer.parseInt(args[2]);
                manager.setIntervalSeconds(id, sec);
                sender.sendMessage(color("&a#" + id + " 개별 간격 설정: &e" + manager.getIntervalSeconds(id) + "초"));
            } catch (NumberFormatException ex) {
                sender.sendMessage(color("&c번호/시간은 정수여야 합니다."));
            }
            return true;
        }
        if (sub.equalsIgnoreCase("기본시간")) {
            if (args.length < 2) { sender.sendMessage(color("&c사용법: /자동공지 기본시간 <초>")); return true; }
            try {
                int sec = Integer.parseInt(args[1]);
                manager.setDefaultIntervalSeconds(sec);
                sender.sendMessage(color("&a기본 간격 설정: &e" + manager.getDefaultIntervalSeconds() + "초"));
            } catch (NumberFormatException ex) {
                sender.sendMessage(color("&c시간은 정수여야 합니다."));
            }
            return true;
        }
        if (sub.equalsIgnoreCase("켜기")) {
            manager.setEnabled(true);
            sender.sendMessage(color("&a자동공지를 켰습니다."));
            return true;
        }
        if (sub.equalsIgnoreCase("끄기")) {
            manager.setEnabled(false);
            sender.sendMessage(color("&c자동공지를 껐습니다."));
            return true;
        }
        sender.sendMessage(color("&c알 수 없는 하위 명령입니다. /자동공지 로 도움말을 확인하세요."));
        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command command, String alias, String[] args) {
        if (!sender.hasPermission("uvp.admin")) { return java.util.Collections.emptyList(); }

        if (args.length == 1) {
            return Arrays.asList("목록","추가","삭제","시간","기본시간","켜기","끄기");
        }
        if (args.length == 2) {
            if (args[0].equalsIgnoreCase("삭제") || args[0].equalsIgnoreCase("추가") || args[0].equalsIgnoreCase("시간")) {
                return Collections.singletonList("1");
            }
            if (args[0].equalsIgnoreCase("기본시간")) {
                return Collections.singletonList("60");
            }
        }
        if (args.length == 3 && args[0].equalsIgnoreCase("시간")) {
            return Collections.singletonList("60");
        }
        return new ArrayList<>();
    }
}
