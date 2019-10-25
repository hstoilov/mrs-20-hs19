pipeline {
	agent any
    tools {
    	maven 'Maven 3.6'
        jdk 'JDK11'
    }
    stages {
    	stage('Version') {
    		agent {
    		    docker 'maven:3-alpine'
    		}
    	    steps {
    	    	echo 'NOTE: this is the maven version of a docker container'
    	        sh 'mvn --version'
    	    }
    	}
    	stage('Checkout') {
        	steps {
            	checkout scm
        		sh 'mvn clean'
			}
		}
        stage('Build') {
        	steps {
        		sh 'mvn verify'
      		}
		}
		stage('Site') {
		    steps {
		        sh 'mvn site'
		    }
		}
		stage('Compile') {
                 steps {
                    sh 'mvn compile'
                 }
		}
		stage('UnitTest') {
                 steps {
					sh 'mvn resources:testResources'
					sh 'mvn compiler:testCompile'
					sh 'mvn surefire:test'
                 }
		}
	}
	post {
	    always {
	        junit 'target/surefire-reports/**/*.xml'
	        junit 'target/failsafe-reports/**/*.xml'
	    }

	}

}