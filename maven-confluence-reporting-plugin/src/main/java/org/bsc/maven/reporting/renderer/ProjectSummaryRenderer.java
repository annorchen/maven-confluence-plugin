package org.bsc.maven.reporting.renderer;

import java.io.File;
import java.io.FileReader;
import java.util.List;
import java.util.Locale;
import org.apache.maven.doxia.sink.Sink;
import org.apache.maven.model.Model;
import org.apache.maven.model.Organization;
import org.apache.maven.model.io.xpp3.MavenXpp3Reader;
import org.apache.maven.project.MavenProject;
import org.apache.maven.reporting.AbstractMavenReportRenderer;
import org.codehaus.plexus.i18n.I18N;

/**
 * 
 * @author Sorrentino
 *
 */
public class ProjectSummaryRenderer extends AbstractMavenReportRenderer {

	private MavenProject project;
	private I18N i18n;
	private Locale locale;

	/**
	 * 
	 * @param owner
	 */
	public ProjectSummaryRenderer(Sink sink, MavenProject project, I18N i18n, Locale locale) {
		super(sink);

		this.project = project;
		this.i18n = i18n;
		this.locale = locale;

	}

	@Override
	public String getTitle() {
		return "Project Summary";
	}

	@Override
	protected void renderBody() {
		startSection(getTitle());

		String name = project.getName();
		if (name == null) {
			name = "";
		}
		String description = project.getDescription();
		if (description == null) {
			description = "";
		}
		String homepage = project.getUrl();
		if (homepage == null) {
			homepage = "";
		}

		startSection(getReportString("report.summary.general.title"));
		startTable();
		tableHeader(new String[] { getReportString("report.summary.field"), getReportString("report.summary.value") });
		tableRow(new String[] { getReportString("report.summary.general.name"), name });
		tableRow(new String[] { getReportString("report.summary.general.description"), description });
		tableRowWithLink(new String[] { getReportString("report.summary.general.homepage"), homepage });
		endTable();
		endSection();

		//organization sub-section
		startSection(getReportString("report.summary.organization.title"));
		Organization organization = project.getOrganization();
		if (organization == null) {
			paragraph(getReportString("report.summary.noorganization"));
		} else {
			if (organization.getName() == null) {
				organization.setName("");
			}
			if (organization.getUrl() == null) {
				organization.setUrl("");
			}

			startTable();
			tableHeader(new String[] { getReportString("report.summary.field"), getReportString("report.summary.value") });
			tableRow(new String[] { getReportString("report.summary.organization.name"), organization.getName() });
			tableRowWithLink(new String[] { getReportString("report.summary.organization.url"), organization.getUrl() });
			endTable();
		}
		endSection();

		//build section
		startSection(getReportString("report.summary.build.title"));
		startTable();
		tableHeader(new String[] { getReportString("report.summary.field"), getReportString("report.summary.value") });
		tableRow(new String[] { getReportString("report.summary.build.groupid"), project.getGroupId() });
		tableRow(new String[] { getReportString("report.summary.build.artifactid"), project.getArtifactId() });
		tableRow(new String[] { getReportString("report.summary.build.version"), project.getVersion() });
		tableRow(new String[] { getReportString("report.summary.build.type"), project.getPackaging() });
		endTable();
		endSection();

		@SuppressWarnings("unchecked")
		List<String> modules = (List<String>) project.getModules();
		if (!modules.isEmpty()) {
			startSection(getReportString("report.summary.modules.title"));

			startTable();
			tableHeader(new String[] { getReportString("report.summary.general.name"), getReportString("report.summary.general.description"),
					getReportString("report.dependencies.repo.locations.title") });

			for (String moduleName : modules) {
				MavenProject moduleProject = getModuleProject(moduleName);
				tableRow(new String[] { moduleName, moduleProject.getDescription(), generateDepedencyInfo(moduleProject) });
			}
			endTable();

			endSection();
		}

		endSection();
	}

	private String generateDepedencyInfo(MavenProject moduleProject) {
		
		StringBuilder text = new StringBuilder();
		
		text.append("<dependency>\n");
		text.append("	<groupID>" + moduleProject.getGroupId() + "</groupID>\n" );
		text.append("	<artifactId>" + moduleProject.getArtifactId() + "</artifactId>\n" );
		text.append("	<version>" + moduleProject.getVersion() + "</version>\n" );
		text.append("</dependency>\n");
		
		return text.toString();
	}

	private MavenProject getModuleProject(String moduleName) {

		Model model = null;
		FileReader reader = null;
		MavenXpp3Reader mavenreader = new MavenXpp3Reader();
		try {
			File pomfile = new File(project.getBasedir(), moduleName + "/pom.xml");
			reader = new FileReader(pomfile);
			model = mavenreader.read(reader);
			model.setPomFile(pomfile);
		} catch (Exception ex) {
		}
		return new MavenProject(model);
	}

	private String getReportString(String key) {
		return i18n.getString("project-info-report", locale, key);
	}

	private void tableRowWithLink(String[] content) {
		sink.tableRow();

		for (int ctr = 0; ctr < content.length; ctr++) {
			String cell = content[ctr];
			if (cell == null) {
				cell = "";
			}

			sink.tableCell();

			if (ctr == content.length - 1 && cell.length() > 0) {
				sink.link(cell);
				sink.text(cell);
				sink.link_();
			} else {
				sink.text(cell);
			}

			sink.tableCell_();
		}

		sink.tableRow_();
	}

}
