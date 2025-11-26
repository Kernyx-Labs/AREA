pipeline {
	agent any

	options {
		disableConcurrentBuilds()
		timestamps()
	}

	environment {
		GIT_AUTHOR_NAME = 'AREA Automation'
		GIT_AUTHOR_EMAIL = 'ci@aincrad-flux.dev'
		SERVER_REPO_URL = 'https://github.com/Aincrad-Flux/AREA-Server.git'
		WEB_REPO_URL = 'https://github.com/Aincrad-Flux/AREA-Web.git'
		MOBILE_REPO_URL = 'https://github.com/Aincrad-Flux/AREA-Mobile.git'
		PUSH_CREDENTIALS_ID = 'Aincrad-Github'
	}

	stages {
		stage('Checkout') {
			steps {
				cleanWs()
				checkout scm
			}
		}

		stage('Server ➜ AREA-Server') {
			when { expression { fileExists('server') } }
			steps {
				withCredentials([
					usernamePassword(
						credentialsId: env.PUSH_CREDENTIALS_ID,
						usernameVariable: 'GIT_PUSH_USERNAME',
						passwordVariable: 'GIT_PUSH_TOKEN'
					)
				]) {
					script {
						syncService(
							serviceLabel: 'server',
							serviceDir: 'server',
							repoUrl: env.SERVER_REPO_URL,
							targetBranch: resolveTargetBranch()
						)
					}
				}
			}
		}

		stage('Web ➜ AREA-Web') {
			when { expression { fileExists('web') } }
			steps {
				withCredentials([
					usernamePassword(
						credentialsId: env.PUSH_CREDENTIALS_ID,
						usernameVariable: 'GIT_PUSH_USERNAME',
						passwordVariable: 'GIT_PUSH_TOKEN'
					)
				]) {
					script {
						syncService(
							serviceLabel: 'web',
							serviceDir: 'web',
							repoUrl: env.WEB_REPO_URL,
							targetBranch: resolveTargetBranch()
						)
					}
				}
			}
		}

		stage('Mobile ➜ AREA-Mobile') {
			when { expression { fileExists('mobile') } }
			steps {
				withCredentials([
					usernamePassword(
						credentialsId: env.PUSH_CREDENTIALS_ID,
						usernameVariable: 'GIT_PUSH_USERNAME',
						passwordVariable: 'GIT_PUSH_TOKEN'
					)
				]) {
					script {
						syncService(
							serviceLabel: 'mobile',
							serviceDir: 'mobile',
							repoUrl: env.MOBILE_REPO_URL,
							targetBranch: resolveTargetBranch()
						)
					}
				}
			}
		}

		stage('Comment') {
			steps {
				script {
					if (env.CHANGE_ID) {
						def date = sh(returnStdout: true, script: 'date -u').trim()
						pullRequest.comment("Split pipeline build ${env.BUILD_ID} ran at ${date}. Server, Web, and Mobile outputs were synced to their dedicated repositories if changes were detected.")
					} else {
						echo 'No pull request detected; skipping comment.'
					}
				}
			}
		}
	}

	post {
		always {
			cleanWs()
		}
	}
}

def resolveTargetBranch() {
	return env.BRANCH_NAME?.trim() ? env.BRANCH_NAME : 'main'
}

def syncService(Map args) {
	String label = args.serviceLabel
	String serviceDir = args.serviceDir
	String repoUrl = args.repoUrl
	String branch = args.targetBranch
	String normalizedDir = serviceDir.startsWith('./') ? serviceDir.substring(2) : serviceDir
	String commitMessage = "Sync ${label} from monorepo build #${env.BUILD_NUMBER ?: 'local'}"
	String repoHostPath = repoUrl.replaceFirst('https://', '')

	if (!fileExists(normalizedDir)) {
		error "Directory '${normalizedDir}' not found for service '${label}'."
	}

	dir("split/${label}") {
		deleteDir()

		withEnv([
			"SERVICE_LABEL=${label}",
			"SERVICE_DIR=${normalizedDir}",
			"SERVICE_REPO=${repoUrl}",
			"SERVICE_BRANCH=${branch}",
			"SERVICE_COMMIT=${commitMessage}",
			"SERVICE_REPO_HOSTPATH=${repoHostPath}"
		]) {
			sh '''#!/bin/bash
			set -euo pipefail
			rsync -a --delete \
				--exclude '.git/' \
				--exclude '.dart_tool/' \
				--exclude 'node_modules/' \
				--exclude 'build/' \
				--exclude '.gradle/' \
				--exclude '.idea/' \
				"$WORKSPACE/$SERVICE_DIR/" ./
			'''

			sh '''#!/bin/bash
			set -euo pipefail
			git init
			git config user.name "$GIT_AUTHOR_NAME"
			git config user.email "$GIT_AUTHOR_EMAIL"
			if git remote | grep -q '^origin$'; then
				git remote remove origin
			fi
			auth_repo="$SERVICE_REPO"
			if [ -n "$GIT_PUSH_USERNAME" ] && [ -n "$GIT_PUSH_TOKEN" ]; then
				auth_repo="https://$GIT_PUSH_USERNAME:$GIT_PUSH_TOKEN@$SERVICE_REPO_HOSTPATH"
			fi
			git remote add origin "$auth_repo"
			git checkout -B "$SERVICE_BRANCH"
			git add -A
			if git diff --cached --quiet; then
				echo "No changes detected for $SERVICE_LABEL; skipping push."
				exit 0
			fi
			git commit -m "$SERVICE_COMMIT"
			git push --force origin "$SERVICE_BRANCH"
			'''
		}
	}
}
