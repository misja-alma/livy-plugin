package org.tera.plugins.livy.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.GuiUtils
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.util.Function
import com.intellij.util.ui.UIUtil
import org.tera.plugins.livy.Settings
import java.awt.BorderLayout
import java.awt.Dimension
import java.util.stream.Collectors
import javax.swing.*

/**
 * TODO options to add:
 * - certificate (option)
 * - checkbox: start new session/ or session id to use
 */
class LivySettingsEditor: SettingsEditor<LivyConfiguration>() {
    private val lineJoiner: Function<MutableList<String>, String> = Function<MutableList<String>, String>  { lines -> lines.stream().collect(Collectors.joining("\n")) }
    private val lineParser: Function<in String, MutableList<String>> = Function<String, MutableList<String>> { lines -> lines.split("\n").toMutableList() }

    private val myPanel: JPanel = JPanel()
    private val hostField: LabeledComponent<JTextField> = LabeledComponent.create(
        GuiUtils.createUndoableTextField(),
        "Host"
    )
    private val sessionIdField: LabeledComponent<JTextField> = LabeledComponent.create(
        GuiUtils.createUndoableTextField(),
        "Sesssion Id"
    )
    private val codeField: LabeledComponent<ExpandableTextField> = LabeledComponent.create(
        ExpandableTextField(lineParser, lineJoiner),
        "Code"
    )
    private val sessionConfigField: LabeledComponent<ExpandableTextField> = LabeledComponent.create(
        ExpandableTextField(lineParser, lineJoiner),
        "Configuration"
    )

    override fun createEditor(): JComponent {
        myPanel.setLayout(VerticalFlowLayout(VerticalFlowLayout.MIDDLE, 0, 5, true, false))
        myPanel.setPreferredSize(Dimension(500, 200))

        hostField.setLabelLocation(BorderLayout.WEST)
        myPanel.add(hostField)
        sessionIdField.setLabelLocation(BorderLayout.WEST)
        myPanel.add(sessionIdField)

        sessionConfigField.setLabelLocation(BorderLayout.WEST)
        myPanel.add(sessionConfigField)
        codeField.setLabelLocation(BorderLayout.WEST)
        myPanel.add(codeField)

        myPanel.updateUI()

        UIUtil.mergeComponentsWithAnchor(hostField, sessionIdField, sessionConfigField, codeField)

        return myPanel
    }

    override fun resetEditorFrom(configuration: LivyConfiguration) {
        hostField.component.text = configuration.host
        sessionIdField.component.text = configuration.sessionId
        codeField.component.text = configuration.code
        sessionConfigField.component.text = configuration.sessionConfig
    }

    override fun applyEditorTo(configuration: LivyConfiguration) {
        configuration.host = hostField.component.text
        configuration.sessionId = sessionIdField.component.text
        configuration.code = codeField.component.text
        configuration.sessionConfig = sessionConfigField.component.text

        Settings.activeSession = configuration.sessionId
        Settings.activeHost = configuration.host
    }
}