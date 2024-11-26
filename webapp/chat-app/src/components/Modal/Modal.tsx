import React from 'react';
import styled from 'styled-components';
import { useSelector, useDispatch } from 'react-redux';
import { RootState } from '../../store';
import { hideModal } from '../../store/slices/uiSlice';

const ModalOverlay = styled.div`
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: rgba(0, 0, 0, 0.5);
  display: flex;
  justify-content: center;
  align-items: center;
  z-index: 1000;
`;

const ModalContent = styled.div`
  background-color: ${({theme}) => theme.colors.surface};
  padding: ${({theme}) => theme.sizing.spacing.lg};
  border-radius: 4px;
  min-width: 300px;
`;

export const Modal: React.FC = () => {
  const dispatch = useDispatch();
  const { modalOpen, modalType } = useSelector((state: RootState) => state.ui);

  if (!modalOpen) return null;

  console.log('[Modal] Rendering modal:', { modalType });

  return (
    <ModalOverlay onClick={() => dispatch(hideModal())}>
      <ModalContent onClick={e => e.stopPropagation()}>
        <h2>{modalType}</h2>
        {/* Add your modal content here based on modalType */}
      </ModalContent>
    </ModalOverlay>
  );
};