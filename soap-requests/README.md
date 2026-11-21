# Sobres SOAP de la demo

Un sobre por caso de la operación `AdoptPet`. El servicio escucha en `http://localhost:8080/ws`, y el contrato se publica en http://localhost:8080/ws/adoption.wsdl.

| Sobre | Resultado esperado |
|---|---|
| `01-adopt-pet.xml` | `AdoptPetResponse` con `status` `ADOPTED` |
| `02-pet-not-available.xml` | Fault `PetNotAvailable` (envíalo después del 01) |
| `03-pet-not-found.xml` | Fault `PetNotFound` |
| `04-blank-owner-name.xml` | Fault de cliente `Validation error` |

Bash:

```bash
curl -s -X POST http://localhost:8080/ws -H "Content-Type: text/xml; charset=utf-8" --data-binary @soap-requests/01-adopt-pet.xml
```

PowerShell:

```powershell
Invoke-WebRequest -Method Post -Uri http://localhost:8080/ws -ContentType "text/xml; charset=utf-8" -InFile soap-requests\01-adopt-pet.xml -SkipHttpErrorCheck | Select-Object -ExpandProperty Content
```

Los faults SOAP se devuelven con el estado HTTP 500, como indica SOAP 1.1. En Windows PowerShell 5.1 no existe `-SkipHttpErrorCheck`: usa `curl.exe` con los mismos argumentos que en bash.
