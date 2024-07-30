{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "pinot.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}


{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "pinot.fullname" -}}
{{- if .Values.fullnameOverride -}}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- $name := default .Chart.Name .Values.nameOverride -}}
{{- if contains $name .Release.Name -}}
{{- .Release.Name | trunc 63 | trimSuffix "-" -}}
{{- else -}}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" -}}
{{- end -}}
{{- end -}}
{{- end -}}


{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "pinot.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Form the Zookeeper URL. If zookeeper is installed in same namespace, use k8s service discovery,
else use user-provided URL
*/}}
{{- define "zookeeper.url" }}
{{- if .Values.zookeeper.url -}}
{{- printf "%s:%s" .Values.zookeeper.url (toString .Values.zookeeper.port) }}
{{- else -}}
{{- printf "zookeeper.%s.svc.cluster.local:%s" .Release.Namespace (toString .Values.zookeeper.port) }}
{{- end -}}
{{- end -}}

{{/*
Form the Zookeeper Path.
*/}}
{{- define "zookeeper.path" }}
{{- if .Values.zookeeper.path -}}
{{- printf "%s%s" (include "zookeeper.url" .) .Values.zookeeper.path }}
{{- else -}}
{{- printf "%s" (include "zookeeper.url" .) }}
{{- end -}}
{{- end -}}

{{/*
Create a default fully qualified pinot controller name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "pinot.controller.fullname" -}}
{{- printf "%s-%s" (include "pinot.fullname" .) .Values.controller.name }}
{{- end -}}


{{/*
Create a default fully qualified pinot broker name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "pinot.broker.fullname" -}}
{{- printf "%s-%s" (include "pinot.fullname" .) .Values.broker.name }}
{{- end -}}

{{/*
Create a default fully qualified pinot minion name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "pinot.minion.fullname" -}}
{{- printf "%s-%s" (include "pinot.fullname" .) .Values.minion.name }}
{{- end -}}

{{/*
Create a default fully qualified pinot server name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
*/}}
{{- define "pinot.server.fullname" -}}
{{- printf "%s-%s" (include "pinot.fullname" .) .Values.server.name }}
{{- end -}}

{{/*
The name of the pinot controller headless service.
*/}}
{{- define "pinot.controller.headless" -}}
{{- printf "%s-headless" (include "pinot.controller.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
The name of the pinot broker headless service.
*/}}
{{- define "pinot.broker.headless" -}}
{{- printf "%s-headless" (include "pinot.broker.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
The name of the pinot minion headless service.
*/}}
{{- define "pinot.minion.headless" -}}
{{- printf "%s-headless" (include "pinot.minion.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
The name of the pinot server headless service.
*/}}
{{- define "pinot.server.headless" -}}
{{- printf "%s-headless" (include "pinot.server.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
The name of the pinot controller external service.
*/}}
{{- define "pinot.controller.external" -}}
{{- printf "%s-external" (include "pinot.controller.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
The name of the pinot broker external service.
*/}}
{{- define "pinot.broker.external" -}}
{{- printf "%s-external" (include "pinot.broker.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
The name of the pinot minion external service.
*/}}
{{- define "pinot.minion.external" -}}
{{- printf "%s-external" (include "pinot.minion.fullname" .) | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "pinot.controller.serviceAccountName" -}}
{{- if .Values.controller.serviceAccount.create -}}
{{ default (include "pinot.controller.fullname" .) .Values.controller.serviceAccount.name }}
{{- else -}}
{{ default "default" .Values.controller.serviceAccount.name }}
{{- end -}}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "pinot.broker.serviceAccountName" -}}
{{- if .Values.broker.serviceAccount.create -}}
{{ default (include "pinot.broker.fullname" .) .Values.broker.serviceAccount.name }}
{{- else -}}
{{ default "default" .Values.broker.serviceAccount.name }}
{{- end -}}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "pinot.minion.serviceAccountName" -}}
{{- if .Values.minion.serviceAccount.create -}}
{{ default (include "pinot.minion.fullname" .) .Values.minion.serviceAccount.name }}
{{- else -}}
{{ default "default" .Values.minion.serviceAccount.name }}
{{- end -}}
{{- end -}}

{{/*
Create the name of the service account to use
*/}}
{{- define "pinot.server.serviceAccountName" -}}
{{- if .Values.server.serviceAccount.create -}}
{{ default (include "pinot.server.fullname" .) .Values.server.serviceAccount.name }}
{{- else -}}
{{ default "default" .Values.server.serviceAccount.name }}
{{- end -}}
{{- end -}}

{{/*
Docker image to use for controller, broker, minion and server
*/}}
{{- define "pinot.image" -}}
  {{- if .Values.global.image.registry -}}
    {{- printf "%s/" .Values.global.image.registry }}
  {{- else if .Values.image.registry -}}
    {{- printf "%s/" .Values.image.registry }}
  {{- end -}}
  {{- if and .Values.image.tagOverride  -}}
    {{- printf "%s:%s" .Values.image.repository .Values.image.tagOverride }}
  {{- else -}}
    {{- printf "%s:%s" .Values.image.repository .Chart.Version }}
  {{- end -}}
{{- end -}}
