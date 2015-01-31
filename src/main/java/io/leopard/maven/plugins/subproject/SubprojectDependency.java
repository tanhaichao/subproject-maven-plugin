package io.leopard.maven.plugins.subproject;

public class SubprojectDependency {

	private String groupId;
	private String artifactId;
	private String scope;

	public String getGroupId() {
		return groupId;
	}

	public void setGroupId(String groupId) {
		this.groupId = groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public void setArtifactId(String artifactId) {
		this.artifactId = artifactId;
	}

	public String getScope() {
		return scope;
	}

	public void setScope(String scope) {
		this.scope = scope;
	}

	@Override
	public String toString() {
		return this.groupId + ":" + this.artifactId + ":" + this.scope;
	}

}
