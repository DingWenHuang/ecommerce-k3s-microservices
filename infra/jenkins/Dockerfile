FROM jenkins/jenkins:lts

USER root

# 1) 安裝必要工具：curl、git、ca-certificates
RUN apt-get update && apt-get install -y --no-install-recommends \
    curl git ca-certificates jq gosu \
 && rm -rf /var/lib/apt/lists/*

# 2) 安裝 kubectl（固定版本可重現）
RUN curl -L -o /usr/local/bin/kubectl \
    https://dl.k8s.io/release/v1.29.8/bin/linux/amd64/kubectl \
 && chmod +x /usr/local/bin/kubectl

# 3) 安裝 k3d
RUN curl -s https://raw.githubusercontent.com/k3d-io/k3d/main/install.sh | bash

# 4) 安裝 Docker CLI（官方 Docker apt 倉庫；Debian Bookworm 已移除 docker.io）
RUN install -m 0755 -d /etc/apt/keyrings \
 && curl -fsSL https://download.docker.com/linux/debian/gpg -o /etc/apt/keyrings/docker.asc \
 && chmod a+r /etc/apt/keyrings/docker.asc \
 && echo "deb [arch=$(dpkg --print-architecture) signed-by=/etc/apt/keyrings/docker.asc] \
     https://download.docker.com/linux/debian \
     $(. /etc/os-release && echo \"$VERSION_CODENAME\") stable" \
     > /etc/apt/sources.list.d/docker.list \
 && apt-get update \
 && apt-get install -y --no-install-recommends docker-ce-cli \
 && rm -rf /var/lib/apt/lists/*

USER root