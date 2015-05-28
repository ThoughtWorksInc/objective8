#!/bin/bash
sudo iptables -A INPUT -i lo -j ACCEPT && \
sudo iptables -A INPUT -m conntrack --ctstate ESTABLISHED,RELATED -j ACCEPT && \
sudo iptables -A INPUT -p tcp --dport 22 -j ACCEPT && \
sudo iptables -A INPUT -p tcp --dport 80 -j ACCEPT && \
sudo iptables -A INPUT -j DROP
