{{/* vim: set filetype=mustache: */}}
{{/*
Expand the name of the chart.
*/}}
{{- define "stream-bootstrap-task.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Create a default fully qualified app name.
We truncate at 63 chars because some Kubernetes name fields are limited to this (by the DNS naming spec).
If release name contains chart name it will be used as a full name.
*/}}
{{- define "stream-bootstrap-task.fullname" -}}
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
{{- define "stream-bootstrap-task.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" -}}
{{- end -}}

{{/*
Common labels
*/}}
{{- define "stream-bootstrap-task.labels" -}}
app.kubernetes.io/name: {{ include "stream-bootstrap-task.name" . }}
helm.sh/chart: {{ include "stream-bootstrap-task.chart" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end -}}

{{- define "stream-bootstrap-task.init-containers" -}}
{{- $fakeRootContext := $ -}}
{{- if or .Values.initContainers .Values.dependencies -}}
initContainers:
{{- if .Values.dependencies -}}
{{- range $dependency := .Values.dependencies }}
  - name: {{ $dependency.serviceName }}-health-check
    image: {{ $dependency.image | default "alpine/curl" }}
    imagePullPolicy: IfNotPresent
    command:
      - sh
      - -c
      - until curl --connect-timeout {{ $dependency.timeout | default "5" }} -s {{ $dependency.protocol | default "http" }}://{{ $dependency.serviceName }}:{{ $dependency.port | default "8080" }}{{ $dependency.path | default "/actuator/health/readiness" }} | grep {{ $dependency.healthIndicator | default "UP" }};
        do echo "Waiting for the {{ $dependency.serviceName }}...";
        sleep {{ $dependency.timeout | default "5" }}; done
{{- end }}
{{- end -}}
{{- if .Values.initContainers -}}
{{ tpl (toYaml .Values.initContainers) . | nindent 2 }}
{{- end -}}
{{- end -}}
{{- end -}}
