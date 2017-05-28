#!/bin/bash

#Make directory for scripts
mkdir scripts

#Change to scripts directory
cd scripts

#Make directory for scrapy
mkdir scrapy

#Change to the scrapy directory
cd scrapy

#Import the GPG key used to sign Scrapy packages into APT keyring
sudo apt-key adv --keyserver hkp://keyserver.ubuntu.com:80 --recv 627220E7

#Enter the password

#Create /etc/apt/sources.list.d/scrapy.list file using the following command
echo 'deb http://archive.scrapy.org/ubuntu scrapy main' | sudo tee /etc/apt/sources.list.d/scrapy.list

#Update package lists and install the scrapy package
sudo apt-get update && sudo apt-get install scrapy

#Install pip
sudo apt-get install python-pip

#Install dev
sudo apt-get install python-dev

#Install scrapy using pip
sudo pip install Scrapy

#Install scrapy-splash
sudo pip install scrapy-splash
