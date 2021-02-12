package org.tera.plugins.livy.services

import com.intellij.openapi.project.Project
import org.tera.plugins.livy.MyBundle

class MyProjectService(project: Project) {

    init {
        println(MyBundle.message("projectService", project.name))
    }
}
