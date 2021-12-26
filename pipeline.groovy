    pipeline{
        agent any
        tools{
            maven 'Maven'
        }
        parameters{
            choice(name: 'Deploy_To', choices: ['DeployDev', 'DeployTest', 'DeployUAT'], description: 'Pick environment')
            //booleanParam(name: 'gitTag', defaultValue: false, description: 'Git tagging ?')
            //booleanParam(name: 'mail', defaultValue: false, description: 'Sent mail ?')
        }
        
        stages{
            
            stage('Development'){
                when{
                    environment name: 'Deploy_To', value: 'DeployDev'
                }
                
                agent any
                
                stages{
                    stage('Git Checkout'){
                        steps{
                            git branch: 'master', url: 'https://github.com/goswami97/testingrepo.git'
                        }
                    }
                    stage('Mail'){
                        steps{
                            script{
                                
                                subject="${currentBuild.projectName} - Build # ${currentBuild.number}"
                                body="<html><body>Hi all,<br><br>CI-CD Pipeline has been initiated.<br><br>Regards,<br>DevOps Team.</body></html>"
                                mail_to="santoshgoswami691@gmail.com"
                                mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '',mimeType: 'text/html', replyTo: '', subject: "${subject}", to: "${mail_to}"
                                
                            }
                        }
                    }
                    stage('Git tag'){
                        steps{
                            script{
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
                        
                                echo $new_tag > /tmp/buildNo
                                
                                '''
                            }
                        }
                    }
                    stage('Code Build'){
                        steps{
                            script{
                                sh '''
                                mvn clean package
                                cp target/*.war /home/projectX
                                '''
                            }
                        }
                    }
                    stage('Docker Image Build and Tag'){
                        steps{
                            withCredentials([usernamePassword(credentialsId: 'dockerCred', passwordVariable: 'dockerPass', usernameVariable: 'dockerID')]) {
                                sh '''
                                new_tag=$(cat /tmp/buildNo)
                                cd /home/projectX/
                                docker login --username "$dockerID" --password "$dockerPass"
                                docker build -t santoshgoswami/samplewebapp:$new_tag .
                                '''
                                
                            }
                        }
                    }
                    stage('Docker image push to docker hub'){
                        steps{
                            sh '''
                            new_tag=$(cat /tmp/buildNo)
                            docker push santoshgoswami/samplewebapp:$new_tag
                            '''
                        }
                    }
                    stage('Deploy container in Remote server'){
                        steps{
                            sh '''
                            new_tag=$(cat /tmp/buildNo)
                            cont_ID=$(ssh jnsadmin@172.30.70.184 'docker ps -qa --filter name=samplewebapp')
                            ssh jnsadmin@172.30.70.184 docker rm "${cont_ID}" -f
                            docker -H ssh://jnsadmin@172.30.70.184 run --name samplewebapp  -d -p 8000:8080 santoshgoswami/samplewebapp:$new_tag
                            '''
                        }
                    }
                    stage('Deployment Status'){
                        steps{
                            sleep 30
                            withCredentials([string(credentialsId: 'githubAccessToken', variable: 'githubAccessToken')]) {
                                sh '''
                                new_tag=$(cat /tmp/buildNo)
                                status=$(curl -so /dev/null -w '%{response_code}' http://172.30.70.184:8000/LoginWebApp-1/) || true
                                if [[ "$status" -eq 200 ]]
                                then
                                    echo "Deployment Successfull"
                                    echo 'SUCCESS' > /tmp/deployment_status.txt
                                    git tag "$new_tag"
                                    git push https://goswami97:"$githubAccessToken"@github.com/goswami97/testingrepo.git --tags
                                else
                                    echo "Deployment Fail"
                                    echo 'FAIL' > /tmp/deployment_status.txt
                                fi
                                '''
                            }   
                        }
                    }
                    stage('Verificatioin'){
                        steps{
                            script{
                                def output=readFile('/tmp/deployment_status.txt').trim()
                                if( "$output" == "FAIL"){
                                    echo "Its Fail"
                                }else{
                                    echo "Its Success"
                                    def ver=readFile('/tmp/buildNo.txt').trim()
                                    
                                    subject= "FDA - Build # ${currentBuild.number} - DEV Deployment Successful"
                                    body="<html><body>Hi all,<br><br>FDA Container has been deployed on DEV environment.<br>URL: http://172.30.70.184:8000/LoginWebApp-1/<br>msg<br><br>Regards,<br>DevOps Team.</body></html>"
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
                                                
                                }
                            }
                        }
                    }
                    
                    
                }
            }
            stage('Testing'){
                when{
                    environment name: 'Deploy_To', value: 'DeployTest'
                }
                
                agent any
                
                stages{
                    stage('Test Stage-1'){
                        steps{
                            echo 'this is test stage 1'
                        }
                    }
                    
                    stage('Test Stage-2'){
                        steps{
                            echo 'this is test stage 2'
                        }
                    }
                    
                    stage('Test Stage-3'){
                        steps{
                            echo 'this is test stage 3'
                        }
                    }
                }
            }
            stage('UAT Environment'){
                when{
                    environment name: 'Deploy_To', value: 'DeployUAT'
                }
                
                agent any
                
                stages{
                    stage('UAT Stage-1'){
                        steps{
                            echo 'this is uat stage 1'
                        }
                    }
                    
                    stage('UAT Stage-2'){
                        steps{
                            echo 'this is uat stage 2'
                        }
                    }
                    
                    stage('UAT Stage-3'){
                        steps{
                            echo 'this is uat stage 3'
                        }
                    }
                }
            }
            

        }
    }