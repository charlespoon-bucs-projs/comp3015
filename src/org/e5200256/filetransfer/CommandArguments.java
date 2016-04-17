package org.e5200256.filetransfer;

import java.util.List;

public final class CommandArguments extends Arguments {

    private final String command;

    public CommandArguments(String[] content) {
        super(content);
        this.command = super.content.remove(0);
    }

    private CommandArguments(List<String> content) {
        super(content);
        this.command = super.content.remove(0);
    }

    public static CommandArguments CreateWithSpliting(String concatd) {
        return new CommandArguments(parse(concatd));
    }

    public String getCommand() {
        return command;
    }
}