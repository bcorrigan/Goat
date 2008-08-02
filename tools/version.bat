@echo off
path %path%;C:\Program Files (x86)\CollabNet Subversion
path %path%;C:\Program Files\CollabNet Subversion
path %path%;C:\Program Files (x86)\Subversion\bin
path %path%;C:\Program Files\Subversion\bin
svn info | findstr Revision