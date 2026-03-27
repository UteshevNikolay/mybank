pipeline {
    agent any

    environment {
        DOCKER_REGISTRY = credentials('docker-registry-url')
        IMAGE_TAG = "${BUILD_NUMBER}"
    }

    stages {
        stage('Validate') {
            steps {
                sh './gradlew compileJava'
            }
        }

        stage('Build') {
            steps {
                sh './gradlew build -x test'
            }
        }

        stage('Test') {
            steps {
                sh './gradlew test'
            }
            post {
                always {
                    junit '**/build/test-results/test/*.xml'
                }
            }
        }

        stage('Docker Build & Push') {
            parallel {
                stage('accounts') {
                    steps {
                        sh "docker build -t ${DOCKER_REGISTRY}/mybank/accounts:${IMAGE_TAG} ./accounts"
                        sh "docker push ${DOCKER_REGISTRY}/mybank/accounts:${IMAGE_TAG}"
                    }
                }
                stage('cash') {
                    steps {
                        sh "docker build -t ${DOCKER_REGISTRY}/mybank/cash:${IMAGE_TAG} ./cash"
                        sh "docker push ${DOCKER_REGISTRY}/mybank/cash:${IMAGE_TAG}"
                    }
                }
                stage('transfer') {
                    steps {
                        sh "docker build -t ${DOCKER_REGISTRY}/mybank/transfer:${IMAGE_TAG} ./transfer"
                        sh "docker push ${DOCKER_REGISTRY}/mybank/transfer:${IMAGE_TAG}"
                    }
                }
                stage('notifications') {
                    steps {
                        sh "docker build -t ${DOCKER_REGISTRY}/mybank/notifications:${IMAGE_TAG} ./notifications"
                        sh "docker push ${DOCKER_REGISTRY}/mybank/notifications:${IMAGE_TAG}"
                    }
                }
                stage('frontend') {
                    steps {
                        sh "docker build -t ${DOCKER_REGISTRY}/mybank/frontend:${IMAGE_TAG} ./frontend"
                        sh "docker push ${DOCKER_REGISTRY}/mybank/frontend:${IMAGE_TAG}"
                    }
                }
            }
        }

        stage('Deploy to Test') {
            steps {
                sh '''
                    helm dependency update helm/mybank
                    helm upgrade --install mybank helm/mybank \
                        -f helm/mybank/values-test.yaml \
                        --set global.accounts.image.repository=${DOCKER_REGISTRY}/mybank/accounts \
                        --set global.accounts.image.tag=${IMAGE_TAG} \
                        --set global.cash.image.repository=${DOCKER_REGISTRY}/mybank/cash \
                        --set global.cash.image.tag=${IMAGE_TAG} \
                        --set global.transfer.image.repository=${DOCKER_REGISTRY}/mybank/transfer \
                        --set global.transfer.image.tag=${IMAGE_TAG} \
                        --set global.notifications.image.repository=${DOCKER_REGISTRY}/mybank/notifications \
                        --set global.notifications.image.tag=${IMAGE_TAG} \
                        --set global.frontend.image.repository=${DOCKER_REGISTRY}/mybank/frontend \
                        --set global.frontend.image.tag=${IMAGE_TAG} \
                        -n test --create-namespace
                '''
            }
        }

        stage('Helm Test') {
            steps {
                sh 'helm test mybank -n test --timeout 120s'
            }
        }

        stage('Approve Production') {
            steps {
                input message: 'Deploy to production?', ok: 'Deploy'
            }
        }

        stage('Deploy to Production') {
            steps {
                sh '''
                    helm upgrade --install mybank helm/mybank \
                        -f helm/mybank/values-prod.yaml \
                        --set global.accounts.image.repository=${DOCKER_REGISTRY}/mybank/accounts \
                        --set global.accounts.image.tag=${IMAGE_TAG} \
                        --set global.cash.image.repository=${DOCKER_REGISTRY}/mybank/cash \
                        --set global.cash.image.tag=${IMAGE_TAG} \
                        --set global.transfer.image.repository=${DOCKER_REGISTRY}/mybank/transfer \
                        --set global.transfer.image.tag=${IMAGE_TAG} \
                        --set global.notifications.image.repository=${DOCKER_REGISTRY}/mybank/notifications \
                        --set global.notifications.image.tag=${IMAGE_TAG} \
                        --set global.frontend.image.repository=${DOCKER_REGISTRY}/mybank/frontend \
                        --set global.frontend.image.tag=${IMAGE_TAG} \
                        -n prod --create-namespace
                '''
            }
        }
    }

    post {
        failure {
            echo 'Pipeline failed!'
        }
        success {
            echo 'Pipeline completed successfully!'
        }
    }
}
