
pipeline {
    agent{node('master')}
    stages {
        stage('Clean workspace & download dist') {
            steps {
                script {
                    cleanWs()
                    withCredentials([
                        usernamePassword(credentialsId: 'srv_sudo',
                        usernameVariable: 'username',
                        passwordVariable: 'password')
                    ]) {
                        try {
                            sh "echo '${password}' | sudo -S docker stop nemytov "
                            sh "echo '${password}' | sudo -S docker container rm nemytov "
                        } catch (Exception e) {
                            print 'container not exist, skip clean'
                        }
                    }
                }
                script {
                    echo 'Update from repository'
                    checkout([$class                           : 'GitSCM',
                              branches                         : [[name: '*/master']],
                              doGenerateSubmoduleConfigurations: false,
                              extensions                       : [[$class           : 'RelativeTargetDirectory',
                                                                   relativeTargetDir: 'auto']],
                              submoduleCfg                     : [],
                              userRemoteConfigs                : [[credentialsId: 'nemytovGit', url: 'https://github.com/ekbhard/study_jenkins.git']]])
                }
            }
        }
        stage ('Build & run docker image'){
            steps{
                script{
                     withCredentials([
                        usernamePassword(credentialsId: 'srv_sudo',
                        usernameVariable: 'username',
                        passwordVariable: 'password')
                    ]) {

                        sh "echo '${password}' | sudo -S docker build ${WORKSPACE}/auto -t nemytov_nginx"
                        sh "echo '${password}' | sudo -S docker run -d -p 8143:80 --name nemytov -v /home/adminci/is_mount_dir:/stat nemytov_nginx"
                    }
                }
            }
        }
        stage ('Get stats & write to file'){
            steps{
                script{
                    withCredentials([
                        usernamePassword(credentialsId: 'srv_sudo',
                        usernameVariable: 'username',
                        passwordVariable: 'password')
                    ]) {
                        
                        sh "echo '${password}' | sudo -S docker exec -t nemytov  bash -c 'df -h > /stat/nemytov.txt'"
                        sh "echo '${password}' | sudo -S docker exec -t nemytov  bash -c 'top -n 1 -b >> /stat/nemytov.txt'"
                    }
                }
            }
        }
        
    }

    
}
