pipeline{

	agent any
	
	parameters{
		choice(
			name: 'environment',
			choices: "deployDev\ndeployTest\ndeployUat",
			description: 'Choose environment'
		)
	}
	
	stages{
		
		stage('Deployment'){
			steps{
				echo "Deployment started"
				script{
					if("$environment" == "deployDev"){
						echo "This is Development stage"
						
						git branch: 'main', url: 'https://goswami97:"$githubAccessToken"@github.com/goswami97/testingrepo.git'
						subject= "${currentBuild.projectName} - Build # ${currentBuild.number}"
						body = '''Hi,
We would like to inform you CICD trigger has been initiated.
						
Regards,
DevOps Team'''
						mail_to = "santoshgoswami691@gmail.com"
						mail bcc: '', body: "${body}", cc: '', charset: 'UTF-8', from: '', replyTo: '', subject: "${subject}", to: "${mail_to}";
						
						sh '''
						#!/bin/bash
						LATEST_TAG=$(git  describe --tag | awk -F "-" '{print $1}')
						IFS='.' # the delimiter is period
						read -ra VERSION <<< "$LATEST_TAG"
						echo "latest tag is $LATEST_TAG"
						echo "major ${VERSION[0]}"
						echo "minor ${VERSION[1]}"
						echo "patch ${VERSION[2]}"
						
						read -ra TAG <<< "$LATEST_TAG"
						echo "incoming git tag ${TAG[0]} ${TAG[1]} ${TAG[2]}"
						patch=$(expr ${TAG[2]} + 1)
						echo "the new patch verison is $patch"
						
						new_tag=""
						new_tag+="${TAG[0]}."
						new_tag+="${TAG[1]}."
						new_tag+="$patch"
						echo "the new git tag generated $new_tag"
																	
						# get latest untagged commit
						commit_id=$(git log --pretty=oneline|head -1| awk '{print $1}')
						echo "the commit id is $commit_id"
						
					
						# push the new tag
						git tag -a "$new_tag" "$commit_id" -m "adding tag"
						git push origin --tag
						
						'''
						
					}else if("$environment" == "deployTest"){
						echo "This is Test stage"
					}else if("$environment" == "deployUat"){
						echo "This is UAT stage"
					}else{
						echo "This is Default"
					}
				}
			}
		}
	}
}
