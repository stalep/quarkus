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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import org.aesh.command.completer.CompleterInvocation;
import org.aesh.command.completer.OptionCompleter;

import io.quarkus.cli.commands.legacy.LegacyQuarkusCommandInvocation;
import io.quarkus.dependencies.Extension;

/**
 * @author <a href="mailto:stalep@gmail.com">Ståle Pedersen</a>
 */
public class ExtensionCompleter implements OptionCompleter<CompleterInvocation> {

    @Override
    public void complete(CompleterInvocation invocation) {
        if (invocation.getGivenCompleteValue().length() == 0) {
            invocation.addAllCompleterValues(
                    getAllExtensions().stream().map(Extension::getSimplifiedArtifactId).collect(Collectors.toList()));
        } else {
            for (Extension loadExtension : getAllExtensions()) {
                if (loadExtension.getSimplifiedArtifactId().startsWith(invocation.getGivenCompleteValue()))
                    invocation.addCompleterValue(loadExtension.getSimplifiedArtifactId());
            }
        }
    }

    private List<Extension> getAllExtensions() {
        try {
            return new ListExtensions(null).loadedExtensions(new LegacyQuarkusCommandInvocation());
        } catch (IOException e) {
            return new ArrayList<>();
        }
    }
}
