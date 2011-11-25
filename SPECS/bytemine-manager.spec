#
# spec file for package bytemine-manager
#
# /*************************************************************************
# * Written by / Copyright (C) 2010 bytemine GmbH                          *
# * Author: Daniel Rauer                     E-Mail:    rauer@bytemine.net *
# *                                                                        *
# * http://www.bytemine.net/                                               *
# *************************************************************************/

%define _topdir		%(pwd)
%define name		bytemine-manager
%define release     1
%define version     2.2.0
%define local_build	%{_topdir}/RPM_BUILD
%define source		%{_topdir}/SOURCES

BuildRoot:  %{buildroot}
Summary:    bytemine-manager
License:    BSD
Vendor:     bytemine GmbH
URL:        http://www.bytemine.net
Name:       %{name}
Version:    %{version}
Release:    %{release}
Source:     %{name}-%{version}.tar
Prefix:     /usr/share/java/bytemine-manager
Group:      Applications/Network
Packager:   Daniel Rauer <rauer@bytemine.net>

#Requires:

#BuildRequires: desktop-file-utils

%description

%prep
mkdir -p %{buildroot}
mkdir -p %{source}
mkdir -p %{buildroot}/usr/share/java/bytemine-manager
mkdir -p %{buildroot}/usr/share/pixmaps

%build
ant -f ../build-manager-rpm.xml

%install
mkdir -p %{buildroot}/usr/share/java/bytemine-manager
cd %{buildroot}/usr/share/java/bytemine-manager
cp %{local_build}/bytemine-manager-%{version}.tar %{buildroot}
tar -xf %{buildroot}/bytemine-manager-%{version}.tar
rm %{buildroot}/bytemine-manager-%{version}.tar

mkdir -p %{buildroot}/usr/share/pixmaps
cp %{_topdir}/bytemine-manager.png %{buildroot}/usr/share/pixmaps/bytemine-manager.png
install %{_topdir}/bytemine-manager.png /usr/share/pixmaps

desktop-file-validate %{_topdir}/bytemine-manager.desktop
install -d -m 755 %{buildroot}%{_datadir}/applications/bytemine
desktop-file-install --dir=%{buildroot}%{_datadir}/applications/bytemine %{_topdir}/bytemine-manager.desktop

%files
/usr/share/java/bytemine-manager/bytemine-manager-%{version}.jar
/usr/share/java/bytemine-manager/start-manager.jar
/usr/share/java/bytemine-manager/lib/
/usr/share/java/bytemine-manager/db/manager.db
/usr/share/java/bytemine-manager/README
/usr/share/java/bytemine-manager/instructions-de.pdf
/usr/share/java/bytemine-manager/instructions-en.pdf
/usr/share/pixmaps/bytemine-manager.png
/usr/share/applications/bytemine/bytemine-manager.desktop

%clean
rm -rf %{source}
rm -rf %{buildroot}/*
rm -rf %{local_build}/*
rm -rf %{_topdir}/RPMS/bytemine-manager-%{version}.tar

