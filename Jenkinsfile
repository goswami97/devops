pipeline{
    agent any
    tools{
        maven 'Maven'
    }
    parameters{
        booleanParam(name: 'sonar', defaultValue: false, description: 'want to do Sonarqube test')
    }

    stages{
        stage('Git checkout & tagging'){
            steps{
                git branch: 'master', url: 'https://github.com/goswami97/testingrepo.git'
                script{
                    subject="${currentBuild.projectName} - Build # ${currentBuild.number}"
                    body="<html><body>Hi all,<br><br>CI-CD Pipeline has been initiated.<br><br>Regards,<br>DevOps Team.</body></html>"
                    mail_to="santoshgoswami691@gmail.com"
                    mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"

                    sh '''
                    LATEST_TAG=$(git  describe --tag | awk -F "-" '{print $1}')
                    major=$(echo "$LATEST_TAG" | awk -F "." '{print $1}')
                    minor=$(echo "$LATEST_TAG" | awk -F "." '{print $2}')
                    patch=$(echo "$LATEST_TAG" | awk -F "." '{print $3}')

                    echo "major $major"
                    echo "minor $minor"
                    echo "patch $patch"

                    newpatch=$(expr $patch + 1)
                    echo "new patch $newpatch"

                    new_tag="${major}.${minor}.${newpatch}"
                    echo "the new git tag generated $new_tag"
                    
                    commit_id=$(git log --pretty=oneline|head -1| awk '{print $1}')
                    
                    echo $commit_id > /tmp/commitID
                    echo $new_tag > /tmp/gitTag     
                    '''
                }
            }
        }
        stage('Backend Code Build'){
            steps{
                script{
                    try{
                        sh '''
                        new_tag=$(cat /tmp/gitTag)
                        sed -i "s/Login Page/Login Page : $new_tag/g" /var/lib/jenkins/workspace/ScriptedPipeline/src/main/webapp/index.jsp
                        mvn clean package > /tmp/buildlog
                        '''
                    } catch(e){
                        currentBuild.result = "FAILED"
				        def build_logs = readFile("/tmp/buildlog")
				        subject= "FDA - Build # ${currentBuild.number} -- Frontend Build ${currentBuild.result}!"
				        body="<html><head></head><body>Hi all,<br><br>Some problem occurred while building code.<br>${build_logs}<br><br>Regards,<br>DevOps Team.</body></html>"
                        mail_to="santoshgoswami691@gmail.com"
				        mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"
				        error('Problem occurred while building UI')
                    }
                }
            }
        }
        stage('Backend SonarQube Test'){
            when{
                environment name: 'sonar', value: 'true'
            }
            steps{
                script{
                    try{
                        sh '''
                        mvn sonar:sonar \
	                        -Dsonar.projectKey=MyTestProject \
	                        -Dsonar.host.url=http://172.30.70.177:9000/sonarqube \
	                        -Dsonar.login=f6fbb4fa7959b2a6bc39bd45aea61e4c31b6f5d7
                        '''
                    }catch(e){
                        currentBuild.result = "FAILED"
				        def sonar_report = readFile("${workspace}/FdaServerParent/target/sonar/report-task.txt")
                        subject= "FDA - Build # ${currentBuild.number} -- Frontend Sonar Test ${currentBuild.result}!"
                        body="<html><head></head><body>Hi all,<br><br>Some problem occurred while sonar scanning for frontend.<br>${sonar_report}.<br>Regards,<br>DevOps Team.</body></html>"
                        mail_to="santoshgoswami691@gmail.com"
				        mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"
				        error('Problem occurred while scanning java code for sonar')
                    }
                }
            }
        }
        stage('Docker Image Build and Tag'){
            steps{
                script{
                    try{
                        withCredentials([usernamePassword(credentialsId: 'dockerCred', passwordVariable: 'dockerPass', usernameVariable: 'dockerID')]) {
                            sh '''
                            new_tag=$(cat /tmp/gitTag)
                            cp /var/lib/jenkins/workspace/ScriptedPipeline/target/*.war /home/projectX
                            cd /home/projectX/
                            docker login --username "$dockerID" --password "$dockerPass"
                            docker build -t santoshgoswami/samplewebapp:$new_tag .
                            '''   
                        }
                    } catch(e){
                        currentBuild.result = "FAILED"
				        subject= "FDA - Build # ${currentBuild.number} -- Frontend Build ${currentBuild.result}!"
				        body="<html><head></head><body>Hi all,<br><br>Some problem occurred while building Docker image.<br><br>Regards,<br>DevOps Team.</body></html>"
                        mail_to="santoshgoswami691@gmail.com"
				        mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"
				        error('Problem occurred while building Docker image')
                    }
                }
            }
        }
        stage('Docker image push to docker hub'){
            steps{
                script{
                    try{
                        sh '''
                        new_tag=$(cat /tmp/gitTag)
                        docker push santoshgoswami/samplewebapp:$new_tag
                        '''
                    } catch(e){
                        currentBuild.result = "FAILED"
				        subject= "FDA - Build # ${currentBuild.number} -- Frontend Build ${currentBuild.result}!"
				        body="<html><head></head><body>Hi all,<br><br>Some problem occurred while sending Docker image to Docker Hub.<br><br>Regards,<br>DevOps Team.</body></html>"
                        mail_to="santoshgoswami691@gmail.com"
				        mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"
				        error('Problem occurred while sending Docker image to Docker Hub')
                    }
                }
            }
        }
        stage('Deploy container in Remote server'){
            steps{
                script{
                    try{
                        sh '''
                        new_tag=$(cat /tmp/gitTag)
                        cont_ID=$(ssh jnsadmin@65.0.20.85 'docker ps -qa --filter name=samplewebapp')
                        ssh jnsadmin@65.0.20.85 docker rm "${cont_ID}" -f
                        docker -H ssh://jnsadmin@65.0.20.85 run --name samplewebapp  -d -p 8000:8080 santoshgoswami/samplewebapp:$new_tag       
                        '''
                    } catch(e){
                        currentBuild.result = "FAILED"
				        subject= "FDA - Build # ${currentBuild.number} -- Frontend Build ${currentBuild.result}!"
				        body="<html><head></head><body>Hi all,<br><br>Some problem occurred while Deploying Docker container to Dev server.<br><br>Regards,<br>DevOps Team.</body></html>"
                        mail_to="santoshgoswami691@gmail.com"
				        mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"
				        error('Problem occurred while Deploying Docker container to Dev server')
                    }
                }
            }
        }
        stage('Deployment Status'){
            steps{
                script{
                    try{
                        echo "Checking url:- http://65.0.20.85:8000/LoginWebApp-1/"
                        sleep 30
                        withCredentials([string(credentialsId: 'githubAccessToken', variable: 'githubAccessToken')]) {
                            sh '''
                            new_tag=$(cat /tmp/gitTag)
                            status=$(curl -so /dev/null -w '%{response_code}' http://65.0.20.85:8000/LoginWebApp-1/) || true
                            if [[ "$status" -eq 200 ]]
                            then
                                echo "Deployment Successfull"
                                echo 'SUCCESS' > /tmp/deployment_status
                                git tag "$new_tag"
                                git push https://goswami97:"$githubAccessToken"@github.com/goswami97/testingrepo.git --tags
                            else
                                echo "Deployment Fail"
                                echo 'FAIL' > /tmp/deployment_status
                            fi
                            '''
                        }
                        def output=readFile('/tmp/deployment_status').trim()
                        if( "$output" == "FAIL"){
                            echo "Deployment Fail"
                            	sh '''
                                current_build=`cat /tmp/gitTag`
                                major=$(echo "$current_build" | awk -F "." '{print $1}')
                                minor=$(echo "$current_build" | awk -F "." '{print $2}')
                                patch=$(echo "$current_build" | awk -F "." '{print $3}')
                                oldpatch=$(expr $patch - 1)
                                previous_build="${major}.${minor}.${oldpatch}"
        
                                echo "Previous tag $previous_build"

                                cont_ID=$(ssh jnsadmin@65.0.20.85 'docker ps -qa --filter name=samplewebapp')
                                ssh jnsadmin@65.0.20.85 docker rm "${cont_ID}" -f
                                docker -H ssh://jnsadmin@65.0.20.85 run --name samplewebapp  -d -p 8000:8080 santoshgoswami/samplewebapp:$previous_build
                                '''
                                subject= "FDA - Build # ${currentBuild.number} - DEV Deployment Successful"
                                body="<html><body>Hi all,<br><br>FDA Container has been deployed on DEV environment.<br>URL: http://172.30.70.184:8000/LoginWebApp-1/<br>Current Deployed FDA Container Version : ${previous_build}<br><br>Regards,<br>DevOps Team.</body></html>"
                                mail_to="santoshgoswami691@gmail.com"
                                mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"

                        }else{
                            echo "Its Success"
                            def ver=readFile('/tmp/gitTag').trim()
                            def msg=readFile('/tmp/commitID').trim()
                                                
                            subject= "FDA - Build # ${currentBuild.number} - DEV Deployment Successful"
                            body="<html><body>Hi all,<br><br>FDA Container has been deployed on DEV environment.<br>URL: http://172.30.70.184:8000/LoginWebApp-1/<br>${msg}<br><br>Regards,<br>DevOps Team.</body></html>"
                            mail_to="santoshgoswami691@gmail.com"
                            mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"

                            subject= "FDA - Build # ${currentBuild.number} -- DEV Revert Request"
                            body= "<html><head><meta http-equiv=Content-Type content=text/html; charset=utf-8><style>.button{background-color:red;border-color:red;border:2px solid red;padding:10px;text-align:center;}.link{display:block;color:#ffffff;font-size:12px;text-decoration:none; text-transform:uppercase;}</style></head><body><br>In order to revert to previous deployment click on below button.<br><br>Current Deployed FDA Container Version : ${ver}<br><br><table><tr><td class=button><a class=link href=#>Revert</a></td><td></td><td></td></tr></table></body></html>"
                            mail_to="santoshgoswami691@gmail.com"
                            mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"
                                                            
                            subject= "FDA - Build # ${currentBuild.number} -- QA Deployment Request"
                            body= "<html><head><meta http-equiv=Content-Type content=text/html; charset=utf-8><style>.button{background-color:green;border-color:green;border:2px solid green;padding:10px;text-align:center;}.link{display:block;color:#ffffff;font-size:12px;text-decoration:none; text-transform:uppercase;}</style></head><body><br>In order to deploy FDA container on QA environment click on below button.<br><br>FDA Container Version: ${ver}<br><br><table><tr><td class=button><a class=link href=#>Approve</a></td><td></td><td></td></tr></table></body></html>"
                            mail_to="santoshgoswami691@gmail.com"
                            mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"
                            
                            echo 'Mail Sent'
                        }

                    } catch(e){
                        currentBuild.result = "FAILED"
                        subject= "FDA - Build # ${currentBuild.number} -- Frontend Build ${currentBuild.result}!"
                        body="<html><head></head><body>Hi all,<br><br>Some problem occurred while checking deployment status for DEV environment.<br><br>Regards,<br>DevOps Team.</body></html>"
                        mail_to="santoshgoswami691@gmail.com"
                        mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"
                        error('Problem occurred while checking deployment on DEV environment')
                    }
                }
            }
        }




    }
}