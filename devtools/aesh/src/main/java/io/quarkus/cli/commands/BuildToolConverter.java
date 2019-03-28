package io.quarkus.cli.commands;

import org.aesh.command.converter.Converter;
import org.aesh.command.converter.ConverterInvocation;

import io.quarkus.generators.BuildTool;

public class BuildToolConverter implements Converter<BuildTool, ConverterInvocation> {
    @Override
    public BuildTool convert(ConverterInvocation invocation) {
        if (invocation.getInput() != null && invocation.getInput().length() > 0)
            return BuildTool.findTool(invocation.getInput());
        else
            return null;
    }
}
