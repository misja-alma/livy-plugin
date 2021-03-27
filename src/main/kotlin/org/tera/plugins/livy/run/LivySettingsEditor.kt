package org.tera.plugins.livy.run

import com.intellij.openapi.options.SettingsEditor
import com.intellij.openapi.ui.LabeledComponent
import com.intellij.openapi.ui.VerticalFlowLayout
import com.intellij.ui.GuiUtils
import com.intellij.ui.components.JBRadioButton
import com.intellij.ui.components.fields.ExpandableTextField
import com.intellij.util.Function
import com.intellij.util.ui.UIUtil
import org.tera.plugins.livy.settings.AppSettingsState
import java.awt.BorderLayout
import java.awt.Dimension
import java.awt.event.ActionEvent
import java.util.stream.Collectors
import javax.swing.JCheckBox
import javax.swing.JComponent
import javax.swing.JPanel
import javax.swing.JTextField

/**
 * TODO field to add:
 * - host certificate (option)
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
    private val sessionNameField: LabeledComponent<JTextField> = LabeledComponent.create(
        GuiUtils.createUndoableTextField(),
        "Sesssion Name"
    )
    private val kindField: LabeledComponent<JTextField> = LabeledComponent.create(
        GuiUtils.createUndoableTextField(),
        "Kind"
    )
    private val driverMemoryField: LabeledComponent<JTextField> = LabeledComponent.create(
        GuiUtils.createUndoableTextField(),
        "Driver Memory"
    )
    private val executorMemoryField: LabeledComponent<JTextField> = LabeledComponent.create(
        GuiUtils.createUndoableTextField(),
        "Executor Memory"
    )
    private val executorCoresField: LabeledComponent<JTextField> = LabeledComponent.create(
        GuiUtils.createUndoableTextField(),
        "Executor Cores"
    )
    private val numberExecutorsField: LabeledComponent<JTextField> = LabeledComponent.create(
        GuiUtils.createUndoableTextField(),
        "Nr. of Executors"
    )
    private val statementTimeoutField: LabeledComponent<JTextField> = LabeledComponent.create(
        GuiUtils.createUndoableTextField(),
        "Statement timeout (sec)"
    )
    private val showRawOutputField: LabeledComponent<JCheckBox> = LabeledComponent.create(
        JCheckBox(),
        "Show Raw Output"
    )
    private val codeField: LabeledComponent<ExpandableTextField> = LabeledComponent.create(
        ExpandableTextField(lineParser, lineJoiner),
        "Code"
    )
    private val sessionConfigField: LabeledComponent<ExpandableTextField> = LabeledComponent.create(
        ExpandableTextField(lineParser, lineJoiner),
        "Configuration"
    )
    private val newSessionRadioButton: JBRadioButton = JBRadioButton("Create New Session")
    private val existingSessionRadioButton: JBRadioButton = JBRadioButton("Use Existing Session")

    override fun createEditor(): JComponent {
        myPanel.layout = VerticalFlowLayout(VerticalFlowLayout.MIDDLE, 0, 5, true, false)
        myPanel.preferredSize = Dimension(500, 500)

        myPanel.add(newSessionRadioButton)
        myPanel.add(existingSessionRadioButton)

        sessionIdField.labelLocation = BorderLayout.WEST
        myPanel.add(sessionIdField)
        sessionNameField.labelLocation = BorderLayout.WEST
        myPanel.add(sessionNameField)
        hostField.labelLocation = BorderLayout.WEST
        myPanel.add(hostField)

        kindField.setLabelLocation(BorderLayout.WEST)
        myPanel.add(kindField)
        driverMemoryField.labelLocation = BorderLayout.WEST
        myPanel.add(driverMemoryField)

        executorMemoryField.labelLocation = BorderLayout.WEST
        myPanel.add(executorMemoryField)
        executorCoresField.labelLocation = BorderLayout.WEST
        myPanel.add(executorCoresField)

        numberExecutorsField.labelLocation = BorderLayout.WEST
        myPanel.add(numberExecutorsField)
        statementTimeoutField.labelLocation = BorderLayout.WEST
        myPanel.add(statementTimeoutField)
        showRawOutputField.labelLocation = BorderLayout.WEST
        myPanel.add(showRawOutputField)

        sessionConfigField.labelLocation = BorderLayout.WEST
        myPanel.add(sessionConfigField)
        codeField.labelLocation = BorderLayout.WEST
        myPanel.add(codeField)

        newSessionRadioButton.addActionListener { e: ActionEvent ->
            sessionIdField.text = ""
            enableOptions(newSessionRadioButton)
        }
        existingSessionRadioButton.addActionListener { e: ActionEvent -> enableOptions(existingSessionRadioButton) }

        myPanel.updateUI()

        UIUtil.mergeComponentsWithAnchor(hostField, sessionIdField, sessionNameField, kindField, driverMemoryField,
            executorMemoryField, executorCoresField, numberExecutorsField, showRawOutputField, sessionConfigField, codeField)

        return myPanel
    }

    override fun resetEditorFrom(configuration: LivyConfiguration) {
        hostField.component.text = configuration.host
        sessionIdField.component.text = idToString(configuration.sessionId)
        sessionNameField.component.text = configuration.sessionName
        kindField.component.text = configuration.kind
        driverMemoryField.component.text = configuration.driverMemory
        executorMemoryField.component.text = configuration.executorMemory
        executorCoresField.component.text = configuration.executorCores.toString()
        numberExecutorsField.component.text = configuration.numExecutors.toString()
        statementTimeoutField.component.text = configuration.statementTimeout.toString()
        showRawOutputField.component.isSelected = configuration.showRawOutput
        codeField.component.text = configuration.code
        sessionConfigField.component.text = configuration.sessionConfig
        if (sessionIdField.component.text.isBlank()) {
            enableOptions(newSessionRadioButton)
        } else {
            enableOptions(existingSessionRadioButton)
        }
    }

    override fun applyEditorTo(configuration: LivyConfiguration) {
        configuration.host = hostField.component.text
        configuration.sessionId = idToInt(sessionIdField.component.text)
        configuration.sessionName = sessionNameField.component.text
        configuration.kind = kindField.component.text
        configuration.driverMemory = driverMemoryField.component.text
        configuration.executorMemory = executorMemoryField.component.text
        executorCoresField.component.text.run { if (this.isNotBlank()) configuration.executorCores = this.toInt() }
        numberExecutorsField.component.text.run { if (this.isNotBlank()) configuration.numExecutors = this.toInt() }
        statementTimeoutField.component.text.run { if (this.isNotBlank()) configuration.statementTimeout = this.toInt() }
        configuration.showRawOutput = showRawOutputField.component.isSelected
        configuration.code = codeField.component.text
        configuration.sessionConfig = sessionConfigField.component.text

        AppSettingsState.activeSession = configuration.sessionId
        AppSettingsState.instance.livyHost = configuration.host
    }

    private fun enableOptions(source: Any) {
        newSessionRadioButton.isSelected = newSessionRadioButton == source
        UIUtil.setEnabled(hostField, newSessionRadioButton == source, true)
        UIUtil.setEnabled(sessionNameField, newSessionRadioButton == source, true)
        UIUtil.setEnabled(kindField, newSessionRadioButton == source, true)
        UIUtil.setEnabled(driverMemoryField, newSessionRadioButton == source, true)
        UIUtil.setEnabled(executorMemoryField, newSessionRadioButton == source, true)
        UIUtil.setEnabled(executorCoresField, newSessionRadioButton == source, true)
        UIUtil.setEnabled(numberExecutorsField, newSessionRadioButton == source, true)
        UIUtil.setEnabled(sessionConfigField, newSessionRadioButton == source, true)

        existingSessionRadioButton.isSelected = existingSessionRadioButton == source
        sessionIdField.isVisible = (existingSessionRadioButton == source)
    }

    private fun idToString(id: Int?): String {
        return id?.toString() ?: ""
    }

    private fun idToInt(id: String?): Int? {
        id?.let {
            return if (it.isBlank()) null else it.toInt()
        }
        return null
    }
}
