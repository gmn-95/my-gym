# my-gym

Aplicativo Android nativo e offline-first para organizar fichas de musculação e executar sessões com cronômetros persistentes.

## Estado atual

- Kotlin e Jetpack Compose com Material 3.
- Tema claro ou escuro seguindo o sistema.
- Textos iniciais em português brasileiro e inglês.
- Room/SQLite com exportação do esquema habilitada.
- Criação, edição, duplicação e exclusão confirmada de fichas.
- Exercícios manuais reutilizáveis em diferentes fichas.
- Séries, repetições fixas ou em faixa e descanso em minutos/segundos.
- Reordenação e remoção dos exercícios de uma ficha.
- Sessão com snapshot da ficha, conclusão individual de cada série e apenas um treino ativo por vez.
- Cronômetro de tempo ativo com pausa explícita.
- Descanso independente com iniciar, pausar, continuar, reiniciar e encerrar.
- Recuperação da sessão após recriação da tela ou encerramento do processo.
- `applicationId` definitivo: `com.gmn95.mygym`.
- Versão atual: `versionCode 4` / `versionName 0.4.0`.
- `minSdk 26`, `targetSdk 37` e `compileSdk 37.0`.

Calendário/histórico, backup JSON e integração com a ExerciseDB ainda não fazem parte desta etapa. As sessões já são persistidas para alimentar o histórico na próxima etapa.

## Uso atual

1. Na tela **Meus treinos**, use **Nova ficha** e informe um nome.
2. Abra a ficha e use **Adicionar exercício**.
3. Crie um exercício manual com nome e observações opcionais, ou reutilize um exercício já cadastrado.
4. Use **Editar** para definir séries, repetições e descanso.
5. Use **Subir** e **Descer** para alterar a ordem dos exercícios.
6. Na tela inicial, uma ficha pode ser aberta, duplicada ou excluída. A exclusão sempre exige confirmação.
7. Use **Iniciar treino** para criar uma sessão baseada em um snapshot da ficha.
8. Durante o treino, marque cada série individualmente. Ao marcar uma série, o descanso configurado para o exercício começa automaticamente.
9. Use **Finalizar treino** para manter a sessão concluída ou **Cancelar treino** para encerrá-la sem tratá-la futuramente como treino realizado.

## Comportamento dos cronômetros

- Bloquear a tela ou trocar de aplicativo não pausa a sessão nem o descanso.
- Os tempos são calculados com o relógio monotônico do Android; não dependem de um contador que precisa permanecer visível.
- Ao colocar o app em segundo plano, os valores são checkpointados no Room sem pausar os cronômetros.
- Se o aparelho for reiniciado, uma sessão e um descanso que estavam ativos são recuperados pausados no último checkpoint. O período com o aparelho desligado não é contado.
- O cronômetro de descanso é independente da pausa da sessão.
- A conclusão total de um exercício é derivada: ele só aparece concluído depois que todas as suas séries forem marcadas.

## Pré-requisitos no Ubuntu/Linux

1. JDK 17 ou mais recente. O Android Gradle Plugin requer no mínimo o JDK 17.
2. Android Studio compatível com o Android Gradle Plugin 9.4, ou Android SDK instalado por linha de comando.
3. No SDK Manager, instalar:
   - Android SDK Platform 37.0;
   - Android SDK Build-Tools 36.0.0 ou mais recente compatível;
   - Android SDK Platform-Tools;
   - Android Emulator, caso queira usar um dispositivo virtual.

Se o SDK estiver fora do local padrão, crie um arquivo `local.properties`, que não deve ser versionado:

```properties
sdk.dir=/caminho/para/Android/Sdk
```

Alternativamente, configure `ANDROID_HOME` no shell.

## Abrir e executar no Android Studio

1. Abra a pasta raiz deste repositório no Android Studio.
2. Aguarde a sincronização do Gradle e a instalação das dependências.
3. Selecione um emulador ou um celular com depuração USB habilitada.
4. Execute a configuração `app` pelo botão **Run**.

## Compilar pela linha de comando

O Gradle Wrapper baixa a versão correta do Gradle automaticamente:

```bash
./gradlew assembleDebug
```

O APK de debug será criado em:

```text
app/build/outputs/apk/debug/app-debug.apk
```

Para instalar ou atualizar o APK de debug em um aparelho conectado:

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
```

Não desinstale o aplicativo antes de uma atualização se quiser preservar seus dados locais.

## Testes e verificações

Os comandos previstos para a etapa inicial são:

```bash
./gradlew testDebugUnitTest
./gradlew lintDebug
./gradlew assembleDebug
./gradlew assembleDebugAndroidTest
```

Os testes unitários validam as configurações dos exercícios e os cálculos monotônicos dos cronômetros. Os testes instrumentados validam as migrações Room, os snapshots por série e os fluxos transacionais de fichas, sessões, pausas, descansos e recuperação após reinicialização. Para executá-los, inicie um emulador ou conecte um celular e use:

```bash
./gradlew connectedDebugAndroidTest
```

## Persistência e futuras atualizações

O banco `my-gym.db` está na versão 4. A migração `1 → 2` cria fichas e exercícios; a migração `2 → 3` cria sessões, snapshots e o estado do descanso; a migração `3 → 4` cria as séries individuais para os snapshots já existentes. Todas preservam os dados anteriores. Os esquemas gerados pelo Room ficam em `app/schemas/` e são versionados. Toda mudança futura do esquema deverá:

1. aumentar a versão do banco;
2. fornecer uma migração Room explícita;
3. preservar os dados existentes;
4. incluir um teste de migração.

Não será usado `fallbackToDestructiveMigration()` em builds do aplicativo. O `applicationId` `com.gmn95.mygym` também não poderá ser alterado. Atualizações release precisarão usar sempre a mesma chave de assinatura e um `versionCode` maior.

## Estrutura

```text
app/src/main/java/com/gmn95/mygym/
├── data/local/       banco Room, entidades, DAOs e migrações
├── data/repository/  contrato e implementação transacional dos dados
├── domain/           validações puras das configurações
├── feature/          ViewModels e estados de tela
├── ui/               telas e raiz da interface Compose
└── ui/theme/         cores e tema Material 3
```

As decisões e convenções obrigatórias para as próximas etapas estão em `AGENTS.md`. A especificação completa está em `SPEC.md`.
