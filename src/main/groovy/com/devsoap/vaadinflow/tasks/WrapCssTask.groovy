/*
 * Copyright 2018-2019 Devsoap Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package com.devsoap.vaadinflow.tasks

import com.devsoap.vaadinflow.extensions.VaadinFlowPluginExtension
import groovy.util.logging.Log
import org.gradle.api.DefaultTask
import org.gradle.api.file.FileTree
import org.gradle.api.tasks.CacheableTask
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectory
import org.gradle.api.tasks.TaskAction

import java.nio.file.Paths

/**
 * Generates corresponding HTML/JS files for CSS files located in frontend/styles
 *
 * @author John Ahlroos
 * @since 1.0
 */
@Log('LOGGER')
@CacheableTask
class WrapCssTask extends DefaultTask {

    static final String NAME = 'vaadinWrapCss'

    static final String STYLES_TARGET_PATH = 'webapp-gen/frontend/styles'
    static final String CSS_REGEXP = '**/*.css'

    private static final String CSS = '.css'
    private static final String FRONTEND = 'frontend'
    private static final String STYLES = 'styles'

    @Optional
    @InputFiles
    final Closure<FileTree> cssFiles = {
        AssembleClientDependenciesTask assembleTask = project.tasks.findByName(AssembleClientDependenciesTask.NAME)
        File stylesPath = Paths.get(assembleTask.webappDir.canonicalPath, FRONTEND, STYLES).toFile()
        stylesPath.exists() ? project.fileTree(stylesPath).matching { it.include(CSS_REGEXP) } : null
    }

    @Optional
    @InputFiles
    final Closure<FileTree> sassCssFiles = {
        File stylesPath = Paths.get(project.buildDir.canonicalPath, 'jsass').toFile()
        stylesPath.exists() ? project.fileTree(stylesPath).matching { it.include(CSS_REGEXP) } : null
    }

    @OutputDirectory
    final File targetPath = new File(project.buildDir, STYLES_TARGET_PATH)

    WrapCssTask() {
        group = 'vaadin'
        description = 'Wraps CSS files in HTML/JS wrappers'
    }

    @TaskAction
    void run() {
        VaadinFlowPluginExtension vaadin = project.extensions.getByType(VaadinFlowPluginExtension)

        if (cssFiles.call()) {
            if (vaadin.compatibilityMode) {
                convertToHtml(cssFiles.call())
            } else {
                convertToJs(cssFiles.call())
            }
        }

        if (sassCssFiles.call()) {
            if (vaadin.compatibilityMode) {
                convertToHtml(sassCssFiles.call())
            } else {
                convertToJs(sassCssFiles.call())
            }
        }
    }

    private void convertToHtml(FileTree tree) {
        tree.each {
            LOGGER.info("Wrapping $it in HTML wrapper")
            String content = """
            <!-- This is a autogenerated html file for ${it.name}. Do not edit this file, it will be overwritten. -->
            <custom-style><style>
            """.stripIndent()

            content += it.text

            content += '\n</style></custom-style>'

            targetPath.mkdirs()
            new File(targetPath, "${ it.name - CSS}.html" ).text = content
        }
    }

    private void convertToJs(FileTree tree) {
        tree.each {
            LOGGER.info("Wrapping $it in JS wrapper")
            String content =
"""
// This is a autogenerated Javascript file for ${it.name}. Do not edit this file, it will be overwritten.
import '@polymer/polymer/lib/elements/custom-style.js';

const \$_documentContainer = document.createElement('template');
\$_documentContainer.innerHTML = `<style>
$it.text
</style>`;
document.head.appendChild(\$_documentContainer.content);
"""
            targetPath.mkdirs()
            new File(targetPath, "${ it.name - CSS}.js" ).text = content
        }
    }
}
