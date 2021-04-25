# Livy Plugin

<!-- Plugin description -->
**Livy Plugin** enables executing code on a Livy server straight from IntelliJ and can show currently running Livy sessions 
with their properties.

<!-- Plugin description end -->

### Table of contents

- [Getting started](#getting-started)
- [Viewing/ managing Livy sessions](#viewing-managing-livy-sessions)
- [Editing settings](#editing-settings)
- [Troubleshooting](#troubleshooting)


## Getting started

After installing the Livy plugin, select the code in your editor that you want to execute on Livy. Then right-click 
and choose 'Run New Livy Session'. This will start a new Livy session and immediately afterwards send your selected
code for execution. The output from Livy will be shown in the output console as soon as the execution has finished.
Unfortunately Livy does not support streaming Spark output as it would be supported by a Spark shell.

![dropdown.png](dropdown.png)

When you want to change any of the Livy properties, right-click and choose 'Modify Run Configuration'.
Alternatively, Livy run configurations can be created and edited in IntelliJ's Run Configuration Dialog.

Once you have created a Livy Session, the plugin will automatically select that session for any new code you want to 
execute. If this is not what you want, you can open the run configuration and choose 'Create New Session'; 
this way the plugin will create a new session next time. 

## Viewing/ managing Livy sessions

To view all Livy's running sessions, go to 'View'/'Tool Windows' and choose 'Livy Sessions'.Livy sessions will be shown 
when 'Refresh' is clicked. After selecting a session it is possible to delete it, but make sure that that session is 
yours! The sessions that are shown are the ones that are running on the Livy server that you configured in your most 
recent Livy run configuration.

## Editing settings

For a new run configuration, the Livy plugin will copy the settings from your last configuration, such as the host to 
connect to and all Spark configuration. You can edit the run configuration to adjust any settings.There is also a 
Livy Settings menu under Preferences-Tools-Livy Settings that lets you edit a few defaults such as the auto-generated 
prefix for the session name.

## Troubleshooting

### There is no 'Run New Livy Session' in the context dialog

First make sure that you have any code selected. If this is the case, it might be that IntelliJ has swapped out the
popup actions from memory. This happens for instance after a IntelliJ restart or when the popup hasn't been used for a 
long time.
Right clicking a couple of times and waiting a couple of seconds should bring back all the dialog actions.

### After choosing 'Run New Livy Session' nothing seems to happen

Check the text and the progress indicator in the status bar of the run console. They will tell if the plugin is still
waiting for the Livy session to be started, or if it is waiting for the execution result.
Or open the sessions panel and choose 'Refresh'. The state of the session that you are using should tell
what is happening, e.g. it could be 'starting' or 'busy'.



