package org.overengineer.inlineproblems.entities;

import com.intellij.openapi.project.Project;
import lombok.AllArgsConstructor;
import lombok.Getter;
import org.overengineer.inlineproblems.entities.enums.ProjectType;


@AllArgsConstructor
@Getter
public class InlineProblemProject {
    private Project project;
    private ProjectType type;
}
