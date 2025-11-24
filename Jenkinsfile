node {
  stage('SCM') {
    checkout scm
  }
  stage('Comment') {
        steps {
            script {
                if (env.CHANGE_ID) {
                    def date = sh(returnStdout: true, script: 'date -u').trim()
                    pullRequest.comment("Build ${env.BUILD_ID} run SonarQube analysis at ${date}")
                } else {
                    echo 'No pull request detected; skipping comment.'
                }
            }
        }
  }
  stage('SonarQube Analysis') {
    def scannerHome = tool 'SonarScanner';
    withSonarQubeEnv() {
      sh "${scannerHome}/bin/sonar-scanner"
    }
  }
  post {
        success {
            echo 'Pipeline completed successfully!'
        }
        failure {
            echo 'Pipeline failed!'
        }
        always {
            cleanWs()
        }
    }
}
