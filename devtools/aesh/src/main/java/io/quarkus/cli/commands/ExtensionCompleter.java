/*
 * Copyright 2019 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.quarkus.cli.commands;

import java.util.stream.Collectors;

import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;

import io.quarkus.dependencies.Extension;
import io.quarkus.maven.utilities.MojoUtils;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class ExtensionCompleter implements OptionCompleter<CompleterInvocation> {

    @Override
    public void complete(CompleterInvocation invocation) {
        if (invocation.getGivenCompleteValue().length() == 0) {
            invocation.addAllCompleterValues(
                    MojoUtils.loadExtensions().stream().map(Extension::getName).collect(Collectors.toList()));
        } else {
            for (Extension loadExtension : MojoUtils.loadExtensions()) {
                if (loadExtension.getName().startsWith(invocation.getGivenCompleteValue()))
                    invocation.addCompleterValue(loadExtension.getName());
            }
        }

    }
}
