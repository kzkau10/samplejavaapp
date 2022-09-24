pipeline {
agent any
stages {
    stage('compile') {
	    steps { 
		    echo 'compiling..'
		    git url: 'https://github.com/lerndevops/samplejavaapp'
		    sh script: '/opt/maven/bin/mvn compile'
	    }
    }
    stage('codereview-pmd') {
	    steps { 
		    echo 'codereview..'
		    sh script: '/opt/maven/bin/mvn -P metrics pmd:pmd'
            }
	    post {
		    success {
			    recordIssues enabledForFailure: true, tool: pmdParser(pattern: '**/target/pmd.xml')
		    }
	    }		
    }
    stage('unit-test') {
	    steps {
		    echo 'unittest..'
		    sh script: '/opt/maven/bin/mvn test'
	    }
	    post {
		    success {
			    junit 'target/surefire-reports/*.xml'
		    }
	    }			
    }
    stage('package/build-war') {
	    steps {
		    echo 'package......'
		    sh script: '/opt/maven/bin/mvn package'	
	    }		
    }
    stage('build & push docker image') {
	    steps {
		    sh 'cd $WORKSPACE'
		    sh 'docker build --file Dockerfile --tag lerndevops/samplejavaapp:$BUILD_NUMBER .'
		    withCredentials([usernamePassword(credentialsId: 'DOCKER_HUB_PWD', passwordVariable: 'PASSWORD', usernameVariable: 'USER')]) {
    			def registry_url = "registry.hub.docker.com/"
              		bat "docker login -u $USER -p $PASSWORD ${registry_url}"
            		docker.withRegistry("http://${registry_url}", "DOCKER_HUB_PWD") {
            			sh 'docker tag lerndevops/samplejavaapp:$BUILD_NUMBER kawal18/lerndevops:latest'
			    	sh 'docker push kawal18/lerndevops'
                }   
		    
	    }
    }
    stage('Deploy-QA') {
	    steps {
		    sh 'ansible-playbook --inventory /tmp/myinv deploy/deploy-kube.yml --extra-vars "env=qa build=$BUILD_NUMBER"'
	    }
    }
}
}
