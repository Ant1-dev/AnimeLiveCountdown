#!/usr/bin/env bash
# One-time setup for the Oracle Cloud Ubuntu 24.04 (Ampere A1) VM.
# Usage (from your machine):
#   scp animeCountdownBackend/deploy/setup-vm.sh animeCountdownBackend/deploy/Caddyfile ubuntu@<VM_IP>:~
#   ssh ubuntu@<VM_IP> 'bash ~/setup-vm.sh'
set -euo pipefail

echo "==> Updating packages"
sudo apt-get update -y
sudo apt-get upgrade -y

echo "==> Installing Docker"
curl -fsSL https://get.docker.com | sudo sh
sudo usermod -aG docker "$USER"   # lets the GitHub deploy run docker without sudo

echo "==> Installing Caddy"
sudo apt-get install -y debian-keyring debian-archive-keyring apt-transport-https curl
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/gpg.key' \
  | sudo gpg --dearmor --yes -o /usr/share/keyrings/caddy-stable-archive-keyring.gpg
curl -1sLf 'https://dl.cloudsmith.io/public/caddy/stable/debian.deb.txt' \
  | sudo tee /etc/apt/sources.list.d/caddy-stable.list
sudo apt-get update -y
sudo apt-get install -y caddy

echo "==> Configuring Caddy"
sudo cp ~/Caddyfile /etc/caddy/Caddyfile
sudo systemctl reload caddy

echo "==> Opening ports 80/443 in the VM firewall (Oracle images block them by default)"
for port in 80 443; do
  if ! sudo iptables -C INPUT -p tcp --dport "$port" -m state --state NEW -j ACCEPT 2>/dev/null; then
    sudo iptables -I INPUT 6 -p tcp --dport "$port" -m state --state NEW -j ACCEPT
  fi
done
sudo netfilter-persistent save

echo "==> Creating app folder for the .env file"
mkdir -p ~/AnimeLiveCountdown

echo
echo "Done. Next:"
echo "  1. Copy your production .env to ~/AnimeLiveCountdown/.env"
echo "  2. Log out and back in (so the docker group applies)"
echo "  3. Make sure the 'api' DNS record points here, then push to main to deploy"
