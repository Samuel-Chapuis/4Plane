package fr.thoridan.planes.entity.custom;

import net.minecraft.network.protocol.Packet;
import net.minecraft.network.protocol.game.ClientGamePacketListener;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import net.minecraftforge.network.NetworkHooks;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import org.jetbrains.annotations.Nullable;

public class YellowPlaneEntity extends Entity {

    private float health = 20.0f; // Points de vie de l’avion
	private float currentSpeed = 0.0f;
	private final float maxSpeed = 1.5f;        // Vitesse maximale de l'avion
	private final float acceleration = 0.02f;   // Vitesse d'accélération
	private final float deceleration = 0.01f;   // Vitesse de décélération
    private float invertSubtlety = 0.3f;        // Sorte de finesse inversé de l'avion
    private float minSpeed = 0.0f;              // Vitesse minimale de l'avion
    private float targetYaw;                    // La rotation Y cible (la direction du joueur)
    private float targetPitch;                  // La rotation X cible (la direction du joueur)
    private float yawSpeed = 2.0f;              // Vitesse d'interpolation pour le yaw
    private float pitchSpeed = 2.0f;            // Vitesse d'interpolation pour le pitch
    private float previousRoll = 0.0f;          // L'angle de roulis précédent
    private float propellerRotation = 0.0F;     // Rotation de l'hélice
    public YellowPlaneEntity(EntityType<? extends Entity> type, Level world) {
        super(type, world);
    }

    private float calculateMaxPitchBasedOnSpeed() {
        return this.currentSpeed * 20f; // Plus la vitesse est élevée, plus le pitch maximal est élevé
    }

    @Override
    protected void defineSynchedData() {
        // Méthode obligatoire pour les entités ; à implémenter selon tes besoins
    }

    @Override
    protected void removePassenger(Entity passenger) {
        super.removePassenger(passenger);
        if (passenger instanceof Player) {
            System.out.println("Le joueur a quitté l'avion.");
            // Code pour gérer la descente, si nécessaire
        }
    }

    @Override
    protected void readAdditionalSaveData(CompoundTag compound) {
        this.health = compound.getFloat("Health"); // Charge la santé lors de la sauvegarde
    }

    private void dropItem() {
        // Remplace Blocks.DIRT par l'item que tu veux faire tomber (par exemple, un item spécifique à l'avion)
        ItemStack itemStack = new ItemStack(Blocks.DIRT);
        ItemEntity itemEntity = new ItemEntity(this.getCommandSenderWorld(), this.getX(), this.getY(), this.getZ(), itemStack);
        this.getCommandSenderWorld().addFreshEntity(itemEntity);
    }

    @Override
    protected void addAdditionalSaveData(CompoundTag compound) {
        compound.putFloat("Health", this.health); // Sauvegarde la santé
    }

    protected void customPositionRider(Entity passenger) {
        if (this.hasPassenger(passenger)) {
            // System.out.println("Le joueur est bien reconnu comme conducteur.");
            double xOffset = 0.0D;
            double yOffset = 0.6D; // Ajuste la hauteur pour que le joueur soit assis correctement
            double zOffset = 0.0D;

            passenger.setPos(this.getX() + xOffset, this.getY() + yOffset, this.getZ() + zOffset);
        } else {
            // System.out.println("Le joueur n'est pas reconnu comme conducteur.");
        }
    }

    @Override
    public Packet<ClientGamePacketListener> getAddEntityPacket() {
        return NetworkHooks.getEntitySpawningPacket(this);
    }

    @Override
    public boolean hurt(DamageSource source, float amount) {
        if (!this.getCommandSenderWorld().isClientSide && this.isAlive()) {
            this.health -= amount;
            System.out.println("Santé de l'avion : " + this.health);

            if (this.health <= 0.0f) {
                this.dropItem();
                this.remove(RemovalReason.KILLED);
            }
        }
        return true;
    }

    @Override
    public boolean isInvulnerableTo(DamageSource source) {
        return false; // Assure-toi que l'entité n'est pas invulnérable
    }

	public boolean isMovingForward() {
		return this.getDeltaMovement().horizontalDistanceSqr() > 0.001;
	}

    @Override
    public boolean isPickable() {
        return true; // Rend l'entité "attrapable"
    }

    @Override
    public boolean isPushable() {
        return true; // Rend l'entité "poussable"
    }

    @Override
    public AABB getBoundingBoxForCulling() {
        return this.getBoundingBox();
    }

    // Gérer l'ajout du conducteur uniquement
    @Override
    public void addPassenger(Entity passenger) {
        // Si le passager est un joueur et que l'avion n'a pas encore de conducteur
        if (passenger instanceof Player && this.getPassengers().isEmpty()) {
            super.addPassenger(passenger);
            System.out.println("Un joueur a été ajouté comme conducteur.");
            this.customPositionRider(passenger);
        }
    }

    @Override
    public boolean canAddPassenger(Entity passenger) {
        // Limite à un seul passager pour cet avion (le conducteur)
        return this.getPassengers().isEmpty();
    }

    @Override
    public double getPassengersRidingOffset() {
        return 0.6D; // Ajuste la hauteur à laquelle le joueur est assis
    }

    // Permettre au joueur de monter dans l'avion
    @Override
    public InteractionResult interact(Player player, InteractionHand hand) {
        if (!this.getCommandSenderWorld().isClientSide) {
            if (this.getPassengers().isEmpty()) {
                System.out.println("Le joueur tente de monter dans l'avion.");
                player.startRiding(this); // Le joueur monte dans l'avion
                return InteractionResult.SUCCESS;
            } else {
                System.out.println("L'avion a déjà un conducteur.");
            }
        }
        return InteractionResult.CONSUME;
    }

	private boolean isPlayerMovingForward(Player player) {
		return player.zza > 0; // `zza` est le champ qui correspond au mouvement vers l'avant
	}

    private boolean isPlayerMovingBackward(Player player) {
        return player.zza < 0; // `zza` est le champ qui correspond au mouvement vers l'arrière
    }

    private void control(){

        if (this.onGround()) {
            minSpeed = 0.0f; // L'avion peut rester immobile au sol
        } else {
            minSpeed = invertSubtlety; // L'avion doit maintenir une vitesse minimale en vol
        }

        if (this.isVehicle() && this.getControllingPassenger() instanceof Player) {
            Player player = (Player) this.getControllingPassenger();

            // Mise à jour des valeurs cibles de rotation en fonction du joueur
            targetYaw = player.getYRot();
            targetPitch = player.getXRot();

            // Limiter le pitch en fonction de la vitesse actuelle
            float maxPitch = calculateMaxPitchBasedOnSpeed();
            if (targetPitch > maxPitch) {
                // targetPitch = maxPitch; // Limite la descente
            }
            else if (targetPitch < -maxPitch) {
                targetPitch = -maxPitch; // Limite la montée
            }

            // Interpolation fluide vers la rotation cible
            float yawDifference = targetYaw - this.getYRot();
            if (Math.abs(yawDifference) > 180.0f) { // Pour gérer la transition à 360°
                yawDifference -= Math.signum(yawDifference) * 360.0f;
            }

            float newYaw = this.getYRot() + Math.min(yawSpeed, Math.abs(yawDifference)) * Math.signum(yawDifference);
            this.setYRot(newYaw);

            float pitchDifference = targetPitch - this.getXRot();
            float newPitch = this.getXRot() + Math.min(pitchSpeed, Math.abs(pitchDifference)) * Math.signum(pitchDifference);
            this.setXRot(newPitch);

            // Synchroniser l'inclinaison X (pitch, verticale) de l'avion avec celle du joueur
            this.setRot(this.getYRot(), this.getXRot());

            // Gestion de l'accélération et de la décélération
            if (isPlayerMovingForward(player)) {
                this.currentSpeed += this.acceleration;
                if (this.currentSpeed > this.maxSpeed) {
                    this.currentSpeed = this.maxSpeed;
                }
            } else if (isPlayerMovingBackward(player)) {
                this.currentSpeed -= this.acceleration;
                if (this.currentSpeed < this.minSpeed) {
                    this.currentSpeed = this.minSpeed; // Bloquer la vitesse à minSpeed pour éviter de reculer
                }
            } else {
                // Lorsque le joueur ne fait rien, on décélère, mais sans passer en dessous de minSpeed
                this.currentSpeed -= this.deceleration;
                if (this.currentSpeed < this.minSpeed) {
                    this.currentSpeed = this.minSpeed; // Bloquer la vitesse à minSpeed pour éviter de reculer
                }
            }

            // Empêcher la vitesse de devenir négative
            if (this.currentSpeed < 0.0f) {
                this.currentSpeed = 0.0f;
            }

            // Calcul du mouvement en fonction de la rotation actuelle de l'avion
            double motionX = -Math.sin(Math.toRadians(this.getYRot())) * this.currentSpeed;
            double motionZ = Math.cos(Math.toRadians(this.getYRot())) * this.currentSpeed;
            double motionY = -Math.sin(Math.toRadians(this.getXRot())) * this.currentSpeed; // Utilise l'inclinaison (pitch) pour déterminer la montée ou la descente

            // Ajout d'une légère force de descente pour éviter de rester en l'air à faible vitesse
            if (this.currentSpeed < 0.3f) {
                motionY -= invertSubtlety;
            }

            this.setDeltaMovement(motionX, motionY, motionZ);
            this.move(MoverType.SELF, this.getDeltaMovement());

        }
    }

    public float getCurrentSpeed() {
        System.out.println("Vitesse actuelle : " + this.currentSpeed);
        return this.currentSpeed;

    }

    @Override
    @Nullable
    public LivingEntity getControllingPassenger() {
        return (LivingEntity) this.getFirstPassenger(); // Retourne le premier passager comme LivingEntity
    }

    public boolean isBeingControlled() {
        return this.getControllingPassenger() instanceof Player;
    }

    public float getRoll() {
        if(this.onGround()){
            return 0.0f;
        }
        float yawDifference = this.targetYaw - this.getYRot();
        if (Math.abs(yawDifference) > 180.0f) { // Pour gérer la transition à 360°
            yawDifference -= Math.signum(yawDifference) * 360.0f;
        }
        return - yawDifference; //*this.getCurrentSpeed()
    }

    public float getPreviousRoll(){
        return this.previousRoll;
    }

    public float getPropellerRotation() {
        return this.propellerRotation;
    }

    public void updatePropellerRotation(float speed) {
        this.propellerRotation += speed * 0.3F; // Ajustez le facteur pour contrôler la vitesse de rotation
        if (this.propellerRotation > 360.0F) {
            this.propellerRotation -= 360.0F; // Réinitialise l'angle si on dépasse 360 degrés
        }
    }

    @Override
    public void tick() {
        super.tick();

        this.previousRoll = this.getRoll();

        this.control();

        if (this.isBeingControlled()) {
            this.updatePropellerRotation(Math.abs(this.getCurrentSpeed()));
        }
    }
}
