# Ateliê Unissex — Android

Aplicativo Android independente que abre o gerador https://perchance.org/atelieuni.

## Recursos nativos
- confirmação de idade (18+), conforme os termos do Perchance;
- WebView com JavaScript, DOM storage e cookies de terceiros habilitados para compatibilidade com o gerador;
- seleção de imagens/arquivos pelo seletor Android;
- downloads de arquivos HTTP(S) para Downloads/AtelieUnissex;
- galeria local dos downloads feitos pelo aplicativo, com abertura e compartilhamento;
- links externos abertos no navegador do sistema;
- ações de voltar, atualizar e compartilhar.

## Build no Cloud Shell
As ferramentas foram instaladas temporariamente em /tmp/atelie-build para preservar a cota do disco persistente.

Com ANDROID_HOME=/tmp/atelie-build/android-sdk, ANDROID_SDK_ROOT igual a ANDROID_HOME e GRADLE_USER_HOME=/tmp/atelie-build/gradle-home, execute:
- /tmp/atelie-build/gradle-9.6/bin/gradle --no-daemon assembleDebug
- APK de teste: app/build/outputs/apk/debug/app-debug.apk

Para o pacote de publicação, gerar o AAB release após configurar uma chave de upload de produção. Não publicar o APK debug.

## Google Play — bloqueio de publicação a resolver
O Google Play não permite app cuja finalidade principal seja apenas apresentar um site em WebView sem permissão do proprietário/administrador do site. Antes de submeter este aplicativo, obter autorização escrita do Perchance para incorporar/publicar o gerador dentro de um app distribuído publicamente e enriquecer/validar a experiência nativa para atender às políticas de funcionalidade mínima. O nome/identidade do app não implica afiliação com Perchance.

Novos apps enviados à Play Store precisam target API 36 (Android 16) desde 31/08/2026.

## Privacidade
Veja PRIVACY-POLICY.md. Antes da publicação, hospede a política em URL pública estável e substitua o contato por um endereço de suporte válido.

## Assinatura de produção
A chave de upload foi criada fora do projeto, em `$HOME/.config/atelie-unissex/atelie-upload.jks`. As credenciais ficam em arquivos locais com permissão 0600 (`signing.properties` e `keystore.pass`) e não devem ser copiadas para Git ou distribuídas. Faça backup seguro do keystore e das credenciais; sem eles, não será possível assinar atualizações com a mesma chave.

Com o Android SDK instalado e as propriedades de assinatura preservadas em `$HOME/.config/atelie-unissex/signing.properties`, use `./gradlew lintRelease assembleRelease bundleRelease`.

## Artefatos gerados
- APK release assinado: `dist/atelie-unissex-1.0.0.apk` (instalação direta no Android).
- Android App Bundle assinado: `dist/atelie-unissex-1.0.0.aab` (formato de envio à Google Play).
- APK debug: `dist/atelie-unissex-debug.apk` (somente testes; pacote com sufixo `.debug`).

A política de privacidade está em `PRIVACY-POLICY.md`. Antes de divulgar publicamente, substituir o contato de suporte placeholder e publicar a política em uma URL HTTPS pública e estável.

Política pública: https://faelafael.github.io/atelie-unissex-android/privacy.html

## Distribuição pública
- APK instalável: baixe `atelie-unissex-1.0.0.apk` na seção Releases do repositório público.
- AAB: formato para Play Console; não é instalável diretamente. A submissão à Play Store depende de autorização do responsável pelo Perchance para o uso do WebView.
- Para instalar o APK: baixe no Android, abra o arquivo e permita a instalação pelo navegador/gerenciador de arquivos quando o sistema solicitar.
- O app não cobra, não exibe publicidade própria e não inclui monetização.
