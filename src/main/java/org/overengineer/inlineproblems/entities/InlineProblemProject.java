package org.overengineer.inlineproblems.entities;

import com.intellij.openapi.project.Project;
import org.overengineer.inlineproblems.entities.enums.ProjectType;


public record InlineProblemProject(Project project, ProjectType type) {
}
