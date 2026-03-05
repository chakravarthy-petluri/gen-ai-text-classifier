{{/*
Expand the name of the chart.
*/}}
{{- define "genai-text-classifier.name" -}}
{{- default .Chart.Name .Values.nameOverride | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Create a default fully qualified app name.
*/}}
{{- define "genai-text-classifier.fullname" -}}
{{- if .Values.fullnameOverride }}
{{- .Values.fullnameOverride | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- $name := default .Chart.Name .Values.nameOverride }}
{{- if contains $name .Release.Name }}
{{- .Release.Name | trunc 63 | trimSuffix "-" }}
{{- else }}
{{- printf "%s-%s" .Release.Name $name | trunc 63 | trimSuffix "-" }}
{{- end }}
{{- end }}
{{- end }}

{{/*
Create chart name and version as used by the chart label.
*/}}
{{- define "genai-text-classifier.chart" -}}
{{- printf "%s-%s" .Chart.Name .Chart.Version | replace "+" "_" | trunc 63 | trimSuffix "-" }}
{{- end }}

{{/*
Common labels
*/}}
{{- define "genai-text-classifier.labels" -}}
helm.sh/chart: {{ include "genai-text-classifier.chart" . }}
{{ include "genai-text-classifier.selectorLabels" . }}
{{- if .Chart.AppVersion }}
app.kubernetes.io/version: {{ .Chart.AppVersion | quote }}
{{- end }}
app.kubernetes.io/managed-by: {{ .Release.Service }}
{{- end }}

{{/*
Selector labels
*/}}
{{- define "genai-text-classifier.selectorLabels" -}}
app.kubernetes.io/name: {{ include "genai-text-classifier.name" . }}
app.kubernetes.io/instance: {{ .Release.Name }}
app: genai-text-classifier
{{- end }}

{{/*
Create the name of the service account to use
*/}}
{{- define "genai-text-classifier.serviceAccountName" -}}
{{- if .Values.serviceAccount.create }}
{{- default (include "genai-text-classifier.fullname" .) .Values.serviceAccount.name }}
{{- else }}
{{- default "default" .Values.serviceAccount.name }}
{{- end }}
{{- end }}